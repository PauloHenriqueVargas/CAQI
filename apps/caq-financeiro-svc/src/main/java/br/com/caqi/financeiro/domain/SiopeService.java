package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.SiopeDtos.DespesasResumo;
import br.com.caqi.financeiro.api.dto.SiopeDtos.ReceitasResumo;
import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopeExportDto;
import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopePendenciaDto;
import br.com.caqi.financeiro.api.dto.SiopeDtos.VinculacaoDetalhe;
import br.com.caqi.financeiro.api.dto.SiopeDtos.Vinculacoes;
import br.com.caqi.financeiro.domain.entity.Despesa;
import br.com.caqi.financeiro.domain.entity.FonteRecurso;
import br.com.caqi.financeiro.domain.entity.Receita;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import br.com.caqi.financeiro.domain.repo.FonteRecursoRepository;
import br.com.caqi.financeiro.domain.repo.ReceitaRepository;
import br.com.caqi.shared.dto.ExecucaoFundebDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gera o quadro consolidado SIOPE para um exercício e detecta pendências
 * que impedem envio limpo ao FNDE.
 *
 * Não há API pública do FNDE para upload — o portal SIOPE recebe arquivos
 * via interface web. Esta service produz o JSON; conversão para o layout
 * de upload (DFCD/MIM-CC) fica para Fase 5.B se houver necessidade.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SiopeService {

    private static final String CLASSE_PESSOAL = "pessoal";
    private static final String CLASSE_CAPITAL = "capital";
    private static final String CLASSE_OUTRAS  = "outras";

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final FonteRecursoRepository fonteRepo;
    private final FundebService fundebService;

    @Value("${caqi.tenant.municipio-id:000000}")
    private String municipioId;

    @Transactional(readOnly = true)
    public SiopeExportDto gerar(int ano) {
        String inicio = String.format("%04d01", ano);
        String fim    = String.format("%04d12", ano);

        List<Receita> receitas = receitaRepo.listarDoAno(inicio, fim);
        List<Despesa> despesas = despesaRepo.listarDoAno(inicio, fim);
        Map<Long, String> fontePorId = new HashMap<>();
        for (FonteRecurso f : fonteRepo.findAll()) {
            fontePorId.put(f.getId(), f.getTipo());
        }

        ExecucaoFundebDto exec = fundebService.calcular(ano);

        ReceitasResumo receitasResumo = consolidarReceitas(receitas);
        DespesasResumo despesasResumo = consolidarDespesas(despesas, fontePorId);
        Vinculacoes vinculacoes = consolidarVinculacoes(exec);
        List<SiopePendenciaDto> pendencias = detectarPendencias(receitas, despesas, fontePorId);

        log.info("SIOPE export ano={} receitas={} despesas={} pendencias={}",
                ano, receitas.size(), despesas.size(), pendencias.size());

        return new SiopeExportDto(ano, municipioId, Instant.now(),
                receitasResumo, despesasResumo, vinculacoes, pendencias);
    }

    private ReceitasResumo consolidarReceitas(List<Receita> receitas) {
        Map<String, BigDecimal> porOrigem = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Receita r : receitas) {
            String origem = (r.getOrigem() != null && !r.getOrigem().isBlank()) ? r.getOrigem() : "indefinida";
            porOrigem.merge(origem, r.getValor(), BigDecimal::add);
            total = total.add(r.getValor());
        }
        return new ReceitasResumo(porOrigem, total);
    }

    private DespesasResumo consolidarDespesas(List<Despesa> despesas, Map<Long, String> fontePorId) {
        Map<String, BigDecimal> porSiopeGrupo = new HashMap<>();
        Map<String, BigDecimal> porFonte = new HashMap<>();
        Map<String, BigDecimal> porClasse = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalMde = BigDecimal.ZERO;

        for (Despesa d : despesas) {
            String siopeGrupo = (d.getSiopeGrupo() != null) ? d.getSiopeGrupo() : "indefinido";
            porSiopeGrupo.merge(siopeGrupo, d.getValor(), BigDecimal::add);

            String fonte = (d.getFonteRecursoId() != null)
                    ? fontePorId.getOrDefault(d.getFonteRecursoId(), "indefinida")
                    : "indefinida";
            porFonte.merge(fonte, d.getValor(), BigDecimal::add);

            String classe = d.isPessoal() ? CLASSE_PESSOAL : (d.isCapital() ? CLASSE_CAPITAL : CLASSE_OUTRAS);
            porClasse.merge(classe, d.getValor(), BigDecimal::add);

            total = total.add(d.getValor());
            if ("MDE".equalsIgnoreCase(d.getSiopeGrupo())) {
                totalMde = totalMde.add(d.getValor());
            }
        }
        return new DespesasResumo(porSiopeGrupo, porFonte, porClasse, totalMde, total);
    }

    private Vinculacoes consolidarVinculacoes(ExecucaoFundebDto exec) {
        return new Vinculacoes(
                new VinculacaoDetalhe(exec.pctMde(), bd("25"),
                        bd("25").subtract(exec.pctMde()), exec.cumpreMde(),
                        "CF/88, art. 212"),
                new VinculacaoDetalhe(exec.pctFundebPessoal(), bd("70"),
                        bd("70").subtract(exec.pctFundebPessoal()), exec.cumpreFundebPessoal(),
                        "Lei 14.113/2020 — Fundeb 70% pessoal"),
                new VinculacaoDetalhe(exec.pctVaatCapital(), bd("15"),
                        bd("15").subtract(exec.pctVaatCapital()), exec.cumpreVaatCapital(),
                        "Lei 14.113/2020 — VAAT 15% capital")
        );
    }

    private List<SiopePendenciaDto> detectarPendencias(List<Receita> receitas, List<Despesa> despesas,
                                                       Map<Long, String> fontePorId) {
        List<SiopePendenciaDto> p = new ArrayList<>();

        for (Receita r : receitas) {
            if (r.getOrigem() == null || r.getOrigem().isBlank()) {
                p.add(new SiopePendenciaDto("RECEITA", r.getId(), "alta",
                        "Receita sem origem definida — não pode ser classificada no SIOPE"));
            }
            if (r.getPcasp() == null || r.getPcasp().isBlank()) {
                p.add(new SiopePendenciaDto("RECEITA", r.getId(), "warn",
                        "Receita sem código PCASP — preencher antes de envio"));
            }
        }

        for (Despesa d : despesas) {
            if (d.getSiopeGrupo() == null || d.getSiopeGrupo().isBlank()) {
                p.add(new SiopePendenciaDto("DESPESA", d.getId(), "alta",
                        "Despesa sem siope_grupo — impede classificação no relatório FNDE"));
            }
            if (d.getNatureza() == null || d.getNatureza().isBlank()) {
                p.add(new SiopePendenciaDto("DESPESA", d.getId(), "alta",
                        "Despesa sem natureza PCASP"));
            }
            if (d.getFonteRecursoId() == null) {
                p.add(new SiopePendenciaDto("DESPESA", d.getId(), "warn",
                        "Despesa sem fonte de recurso — recomendado para rastreabilidade Fundeb"));
            }
            // Cross-check: capital com fonte != VAAT pode estar mal classificada
            if (d.isCapital() && d.getFonteRecursoId() != null) {
                String fonte = fontePorId.get(d.getFonteRecursoId());
                if (fonte != null && !"VAAT".equals(fonte)) {
                    p.add(new SiopePendenciaDto("DESPESA", d.getId(), "info",
                            String.format("Despesa de capital com fonte %s — verifique se deveria ser VAAT (15%%)", fonte)));
                }
            }
        }

        return p;
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
