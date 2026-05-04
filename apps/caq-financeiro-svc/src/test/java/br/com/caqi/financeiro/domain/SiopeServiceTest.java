package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopeExportDto;
import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopePendenciaDto;
import br.com.caqi.financeiro.domain.entity.Despesa;
import br.com.caqi.financeiro.domain.entity.FonteRecurso;
import br.com.caqi.financeiro.domain.entity.Receita;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import br.com.caqi.financeiro.domain.repo.FonteRecursoRepository;
import br.com.caqi.financeiro.domain.repo.ReceitaRepository;
import br.com.caqi.shared.dto.ExecucaoFundebDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiopeServiceTest {

    private ReceitaRepository receitaRepo;
    private DespesaRepository despesaRepo;
    private FonteRecursoRepository fonteRepo;
    private FundebService fundebService;
    private SiopeService svc;

    @BeforeEach
    void setUp() {
        receitaRepo = mock(ReceitaRepository.class);
        despesaRepo = mock(DespesaRepository.class);
        fonteRepo = mock(FonteRecursoRepository.class);
        fundebService = mock(FundebService.class);
        svc = new SiopeService(receitaRepo, despesaRepo, fonteRepo, fundebService);
        ReflectionTestUtils.setField(svc, "municipioId", "1234567");
    }

    @Test
    @DisplayName("Cenário-base do seed (cumpre tudo) — agrega receitas/despesas e zero pendências")
    void cenarioCompleto_agregaSemPendencias() {
        when(receitaRepo.listarDoAno(any(), any())).thenReturn(List.of(
                receita(1L, "202503", "100000", "impostos",       "1.7.1.0.00.00"),
                receita(2L, "202504",  "80000", "transferencias", "1.7.2.0.00.00"),
                receita(3L, "202505", "200000", "Fundeb_VAAF",    "1.7.5.1.00.00"),
                receita(4L, "202506",  "50000", "Fundeb_VAAT",    "1.7.5.2.00.00"),
                receita(5L, "202507",  "30000", "Fundeb_VAAR",    "1.7.5.3.00.00")
        ));
        when(despesaRepo.listarDoAno(any(), any())).thenReturn(List.of(
                despesa(1L, "3.1.90.11", "200000", 100L, "MDE"),  // pessoal VAAF
                despesa(2L, "3.1.90.11",  "30000", 102L, "MDE"),  // pessoal VAAR
                despesa(3L, "3.3.90.30",  "20000", 103L, "MDE"),  // material Propria
                despesa(4L, "4.4.90.52",  "10000", 101L, "MDE")   // capital VAAT
        ));
        when(fonteRepo.findAll()).thenReturn(List.of(
                fonte(100L, "VAAF"), fonte(101L, "VAAT"), fonte(102L, "VAAR"), fonte(103L, "Propria")));
        when(fundebService.calcular(2025)).thenReturn(execCumpre());

        SiopeExportDto r = svc.gerar(2025);

        assertThat(r.ano()).isEqualTo(2025);
        assertThat(r.tenantMunicipioId()).isEqualTo("1234567");

        // Receitas
        assertThat(r.receitas().total()).isEqualByComparingTo("460000");
        assertThat(r.receitas().porOrigem()).containsEntry("impostos",       new BigDecimal("100000"));
        assertThat(r.receitas().porOrigem()).containsEntry("Fundeb_VAAF",    new BigDecimal("200000"));

        // Despesas: total 260k, MDE 260k
        assertThat(r.despesas().total()).isEqualByComparingTo("260000");
        assertThat(r.despesas().totalMde()).isEqualByComparingTo("260000");
        assertThat(r.despesas().porClasse()).containsEntry("pessoal", new BigDecimal("230000"));
        assertThat(r.despesas().porClasse()).containsEntry("capital", new BigDecimal("10000"));
        assertThat(r.despesas().porClasse()).containsEntry("outras",  new BigDecimal("20000"));
        assertThat(r.despesas().porFonteRecurso()).containsEntry("VAAF", new BigDecimal("200000"));

        // Vinculações
        assertThat(r.vinculacoes().mde25().cumpre()).isTrue();
        assertThat(r.vinculacoes().fundeb70().cumpre()).isTrue();
        assertThat(r.vinculacoes().vaat15().cumpre()).isTrue();
        assertThat(r.vinculacoes().mde25().baseLegal()).contains("CF/88");

        // Sem pendências
        assertThat(r.pendencias()).isEmpty();
    }

    @Test
    @DisplayName("Despesa sem siope_grupo, sem natureza, sem fonte → 3 pendências; capital fora de VAAT → info")
    void detectaPendencias() {
        when(receitaRepo.listarDoAno(any(), any())).thenReturn(List.of(
                receita(1L, "202501", "100", null, null)  // sem origem + sem PCASP
        ));
        when(despesaRepo.listarDoAno(any(), any())).thenReturn(List.of(
                despesa(1L, null, "100", null, null),                    // sem natureza, sem siope_grupo, sem fonte
                despesa(2L, "4.4.90.52", "5000", 100L, "MDE")            // capital com fonte VAAF (não VAAT)
        ));
        when(fonteRepo.findAll()).thenReturn(List.of(fonte(100L, "VAAF")));
        when(fundebService.calcular(2025)).thenReturn(execCumpre());

        SiopeExportDto r = svc.gerar(2025);

        // Receita: 1 alta (sem origem) + 1 warn (sem PCASP)
        long receitaAltas = countSeveridade(r.pendencias(), "RECEITA", "alta");
        long receitaWarns = countSeveridade(r.pendencias(), "RECEITA", "warn");
        assertThat(receitaAltas).isEqualTo(1);
        assertThat(receitaWarns).isEqualTo(1);

        // Despesa 1: sem siope_grupo (alta) + sem natureza (alta) + sem fonte (warn) = 3
        // Despesa 2: capital com fonte != VAAT → 1 info
        long despesaAltas = countSeveridade(r.pendencias(), "DESPESA", "alta");
        long despesaInfos = countSeveridade(r.pendencias(), "DESPESA", "info");
        assertThat(despesaAltas).isGreaterThanOrEqualTo(2);
        assertThat(despesaInfos).isEqualTo(1);
    }

    private static long countSeveridade(List<SiopePendenciaDto> pend, String entidade, String severidade) {
        return pend.stream()
                .filter(p -> entidade.equals(p.tipoEntidade()) && severidade.equals(p.severidade()))
                .count();
    }

    private static Receita receita(Long id, String comp, String valor, String origem, String pcasp) {
        Receita r = new Receita();
        r.setId(id); r.setCompetencia(comp); r.setValor(new BigDecimal(valor));
        r.setOrigem(origem); r.setPcasp(pcasp);
        return r;
    }

    private static Despesa despesa(Long id, String natureza, String valor, Long fonteId, String siopeGrupo) {
        Despesa d = new Despesa();
        d.setId(id); d.setCompetencia("202501"); d.setNatureza(natureza);
        d.setValor(new BigDecimal(valor)); d.setFonteRecursoId(fonteId); d.setSiopeGrupo(siopeGrupo);
        return d;
    }

    private static FonteRecurso fonte(Long id, String tipo) {
        FonteRecurso f = new FonteRecurso();
        f.setId(id); f.setTipo(tipo);
        return f;
    }

    private static ExecucaoFundebDto execCumpre() {
        return new ExecucaoFundebDto(2025,
                new BigDecimal("180000"), new BigDecimal("280000"), new BigDecimal("50000"),
                new BigDecimal("260000"), new BigDecimal("230000"), new BigDecimal("10000"),
                new BigDecimal("144.44"), new BigDecimal("82.14"), new BigDecimal("20.00"),
                true, true, true);
    }
}
