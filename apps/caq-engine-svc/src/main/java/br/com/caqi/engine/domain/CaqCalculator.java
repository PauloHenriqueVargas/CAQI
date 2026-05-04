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
 * Motor de cálculo CAQ/CAQi — função pura sobre o estado do banco.
 *
 * Para cada (escola, etapa) na requisição:
 *   1. Carrega parâmetros vigentes da etapa (alunos_por_turma).
 *   2. Conta total de matrículas ativas na escola.
 *   3. Para cada insumo aplicável à etapa com custo vigente:
 *        custo_anual    = qtd_padrao × custo_unitario
 *        divisor        = 1 (por_aluno) | alunos_por_turma (por_turma) | total_alunos_escola (por_escola)
 *        custo_aluno_ano = custo_anual / divisor   (HALF_UP, scale 4)
 *   4. CAQi/aluno/ano = soma de todos os custo_aluno_ano (HALF_UP, scale 2 no agregado).
 *
 * NOTA Fase 3: a distinção CAQ adequado vs CAQi mínimo exigirá uma coluna 'perfil'
 * em custo_insumo (ou catálogo de insumos paralelo). Hoje CAQ = CAQi (mesmo valor).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CaqCalculator {

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

                for (Insumo insumo : todosInsumos) {
                    if (!insumo.aplicaSeAEtapa(etapaCodigo)) continue;

                    Optional<CustoInsumo> custoOpt = custoRepo.findVigente(insumo.getId(), dataReferencia);
                    if (custoOpt.isEmpty()) {
                        log.warn("Insumo {} sem custo vigente em {} — pulando", insumo.getCodigo(), req.ano());
                        continue;
                    }
                    CustoInsumo custo = custoOpt.get();

                    BigDecimal custoAnual = insumo.getQtdPadrao().multiply(custo.getCustoUnitario());
                    Divisor divisor = calcularDivisor(insumo, params, totalAlunosEscola, escolaIdStr);
                    if (divisor == null) continue;

                    BigDecimal custoAlunoAno = custoAnual.divide(divisor.valor(), 4, RoundingMode.HALF_UP);
                    totalCaqi = totalCaqi.add(custoAlunoAno);

                    String baseCalculo = String.format(
                            "R$ %s × %s %s / %s = R$ %s/aluno/ano",
                            custo.getCustoUnitario().toPlainString(),
                            insumo.getQtdPadrao().toPlainString(),
                            insumo.getUnidade(),
                            divisor.descricao(),
                            custoAlunoAno.toPlainString()
                    );

                    memoria.add(new ItemMemoriaCalculoDto(
                            escolaIdStr, etapaCodigo,
                            insumo.getCodigo(), insumo.getNome(), insumo.getTipoAplicacao(),
                            insumo.getQtdPadrao(), custo.getCustoUnitario(),
                            custoAnual, divisor.valor(), custoAlunoAno, baseCalculo
                    ));
                }

                BigDecimal caqi = totalCaqi.setScale(2, RoundingMode.HALF_UP);
                itens.add(new ItemResultadoDto(
                        escolaIdStr, etapaCodigo,
                        caqi,                     // CAQi
                        caqi,                     // CAQ (Fase 3 — distinguir)
                        BigDecimal.ZERO           // gap (Fase 3 — comparar com execução)
                ));
            }
        }

        return new ResultadoCalculoDto(req.ano(), itens, memoria);
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
