package br.com.caqi.engine.domain;

import br.com.caqi.engine.api.dto.ItemMemoriaCalculoDto;
import br.com.caqi.engine.api.dto.ItemResultadoDto;
import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import br.com.caqi.engine.domain.entity.CustoInsumo;
import br.com.caqi.engine.domain.entity.Escola;
import br.com.caqi.engine.domain.entity.Etapa;
import br.com.caqi.engine.domain.entity.Insumo;
import br.com.caqi.engine.domain.entity.ParametroEtapa;
import br.com.caqi.engine.domain.repo.CustoInsumoRepository;
import br.com.caqi.engine.domain.repo.EscolaRepository;
import br.com.caqi.engine.domain.repo.EtapaRepository;
import br.com.caqi.engine.domain.repo.InsumoRepository;
import br.com.caqi.engine.domain.repo.MatriculaRepository;
import br.com.caqi.engine.domain.repo.ParametroEtapaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Motor de cálculo CAQ/CAQi.
 *
 * Para cada (escola, etapa) na requisição, calcula DOIS valores:
 *   - CAQi (perfil="minimo")  — piso mínimo de qualidade (PNE Meta 20)
 *   - CAQ  (perfil="adequado") — padrão adequado de qualidade
 * E o gap = CAQ − CAQi (R$/aluno/ano que falta para alcançar o adequado).
 *
 * Algoritmo por insumo:
 *   custo_anual    = qtd_padrao × custo_unitario_do_perfil
 *   divisor        = 1 (por_aluno) | alunos_por_turma | total_alunos_escola
 *   custo_aluno_ano = custo_anual / divisor   (HALF_UP, scale 4)
 * Soma por perfil, arredonda HALF_UP scale 2 no agregado final.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CaqCalculator {

    private static final List<String> PERFIS = List.of(Perfil.MINIMO, Perfil.ADEQUADO);

    private final EtapaRepository etapaRepo;
    private final ParametroEtapaRepository parametroRepo;
    private final InsumoRepository insumoRepo;
    private final CustoInsumoRepository custoRepo;
    private final EscolaRepository escolaRepo;
    private final MatriculaRepository matriculaRepo;

    public ResultadoCalculoDto calcular(RequisicaoCalculoDto req) {
        LocalDate dataReferencia = LocalDate.of(req.ano(), 1, 1);
        List<Insumo> todosInsumos = insumoRepo.findAll();

        List<ItemResultadoDto> itens = new ArrayList<>();
        List<ItemMemoriaCalculoDto> memoria = new ArrayList<>();

        for (String escolaIdStr : req.escolas()) {
            Long escolaId = parseEscolaId(escolaIdStr);
            Escola escola = escolaRepo.findById(escolaId)
                    .orElseThrow(() -> new IllegalArgumentException("Escola não encontrada: " + escolaIdStr));
            long totalAlunosEscola = matriculaRepo.contarAtivasPorEscola(escola.getId());

            for (String etapaCodigo : req.etapas()) {
                Etapa etapa = etapaRepo.findByCodigo(etapaCodigo)
                        .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada: " + etapaCodigo));
                ParametroEtapa params = parametroRepo.findVigente(etapa.getId(), dataReferencia)
                        .orElseThrow(() -> new IllegalStateException(
                                "Sem parâmetros vigentes para etapa " + etapaCodigo + " em " + req.ano()));

                BigDecimal totalCaqi = BigDecimal.ZERO;
                BigDecimal totalCaq = BigDecimal.ZERO;

                for (Insumo insumo : todosInsumos) {
                    if (!insumo.aplicaSeAEtapa(etapaCodigo)) continue;

                    for (String perfil : PERFIS) {
                        Optional<CustoInsumo> custoOpt = custoRepo.findVigente(insumo.getId(), perfil, dataReferencia);
                        if (custoOpt.isEmpty()) {
                            // Para perfil 'adequado', a ausência é esperada se não houver dado;
                            // para 'minimo' loga warning porque a planilha base sempre tem.
                            if (Perfil.MINIMO.equals(perfil)) {
                                log.warn("Insumo {} sem custo vigente perfil={} em {} — pulando",
                                        insumo.getCodigo(), perfil, req.ano());
                            }
                            continue;
                        }
                        CustoInsumo custo = custoOpt.get();

                        Calculado c = calcularContribuicao(
                                escolaIdStr, etapaCodigo, perfil, insumo, custo, params, totalAlunosEscola);
                        if (c == null) continue;

                        memoria.add(c.memoria());
                        if (Perfil.MINIMO.equals(perfil)) {
                            totalCaqi = totalCaqi.add(c.custoAlunoAno());
                        } else {
                            totalCaq = totalCaq.add(c.custoAlunoAno());
                        }
                    }
                }

                BigDecimal caqi = totalCaqi.setScale(2, RoundingMode.HALF_UP);
                BigDecimal caq  = totalCaq.setScale(2, RoundingMode.HALF_UP);
                BigDecimal gap  = caq.subtract(caqi);

                itens.add(new ItemResultadoDto(escolaIdStr, etapaCodigo, caqi, caq, gap));
            }
        }

        return new ResultadoCalculoDto(req.ano(), itens, memoria);
    }

    private record Calculado(BigDecimal custoAlunoAno, ItemMemoriaCalculoDto memoria) {}

    private Calculado calcularContribuicao(
            String escolaIdStr, String etapaCodigo, String perfil,
            Insumo insumo, CustoInsumo custo, ParametroEtapa params, long totalAlunosEscola
    ) {
        BigDecimal custoAnual = insumo.getQtdPadrao().multiply(custo.getCustoUnitario());
        Divisor divisor = calcularDivisor(insumo, params, totalAlunosEscola, escolaIdStr);
        if (divisor == null) return null;

        BigDecimal custoAlunoAno = custoAnual.divide(divisor.valor(), 4, RoundingMode.HALF_UP);
        String baseCalculo = String.format(
                "[%s] R$ %s × %s %s / %s = R$ %s/aluno/ano",
                perfil,
                custo.getCustoUnitario().toPlainString(),
                insumo.getQtdPadrao().toPlainString(),
                insumo.getUnidade(),
                divisor.descricao(),
                custoAlunoAno.toPlainString()
        );

        return new Calculado(custoAlunoAno, new ItemMemoriaCalculoDto(
                escolaIdStr, etapaCodigo, perfil,
                insumo.getCodigo(), insumo.getNome(), insumo.getTipoAplicacao(),
                insumo.getQtdPadrao(), custo.getCustoUnitario(),
                custoAnual, divisor.valor(), custoAlunoAno, baseCalculo
        ));
    }

    private record Divisor(BigDecimal valor, String descricao) {}

    private Divisor calcularDivisor(Insumo insumo, ParametroEtapa params, long totalEscola, String escolaIdStr) {
        return switch (insumo.getTipoAplicacao()) {
            case "por_aluno"  -> new Divisor(BigDecimal.ONE, "1 aluno");
            case "por_turma"  -> new Divisor(params.getAlunosPorTurma(),
                    params.getAlunosPorTurma().toPlainString() + " alunos/turma");
            case "por_escola" -> {
                if (totalEscola == 0) {
                    log.warn("Escola {} sem matrículas ativas — pulando insumo {}", escolaIdStr, insumo.getCodigo());
                    yield null;
                }
                yield new Divisor(BigDecimal.valueOf(totalEscola), totalEscola + " alunos da escola");
            }
            default -> {
                log.warn("Tipo de aplicação desconhecido: {} (insumo {})",
                        insumo.getTipoAplicacao(), insumo.getCodigo());
                yield null;
            }
        };
    }

    private Long parseEscolaId(String s) {
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("escolaId deve ser numérico (id interno): " + s);
        }
    }
}
