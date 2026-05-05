package br.com.caqi.escolar.censo;

import br.com.caqi.escolar.core.TenantProperties;
import br.com.caqi.escolar.domain.entity.CensoImportacao;
import br.com.caqi.escolar.domain.entity.CensoMatricula;
import br.com.caqi.escolar.domain.repo.CensoImportacaoRepository;
import br.com.caqi.escolar.domain.repo.CensoMatriculaRepository;
import br.com.caqi.escolar.domain.repo.EscolaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test do CensoMatriculaImportService usando o fixture MATRICULA_FIXTURE.csv (12 linhas).
 *
 * Cenário esperado para tenant Palmas/TO (1721000) com salt "salt-test-1234":
 *  - 12 registros processados
 *  - 8 matrículas válidas em escolas municipais ativas com etapa mapeável:
 *      CRECHE=2, EF1=3, EF2=1, EM=1, EJA=1
 *  - 2 ignoradas por dependência ≠ municipal (estadual + privada)
 *  - 1 ignorada por etapa não mapeada (TP_ETAPA_ENSINO=77)
 *  - 1 ignorada silenciosamente por município ≠ alvo (1722000)
 *  - 2 com NEE (deficiência intelectual + autismo)
 */
@DisplayName("CensoMatriculaImportService — pseudonimização + filtros + batch insert")
class CensoMatriculaImportServiceTest {

    private static final String SALT = "salt-test-12345678";

    private CensoMatriculaParser parser;
    private CensoImportacaoRepository importacaoRepo;
    private CensoMatriculaRepository matriculaRepo;
    private EscolaRepository escolaRepo;
    private CensoMatriculaImportService service;

    @BeforeEach
    void setUp() {
        parser = new CensoMatriculaParser();
        importacaoRepo = mock(CensoImportacaoRepository.class);
        matriculaRepo = mock(CensoMatriculaRepository.class);
        escolaRepo = mock(EscolaRepository.class);
        TenantProperties tenant = new TenantProperties("172100", "Palmas", "1721000");

        AtomicLong idGen = new AtomicLong(0);
        when(importacaoRepo.saveAndFlush(any())).thenAnswer(inv -> {
            CensoImportacao imp = inv.getArgument(0);
            imp.setId(idGen.incrementAndGet() + 1000);
            return imp;
        });
        when(importacaoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matriculaRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(escolaRepo.findByInepId(any())).thenReturn(Optional.empty());

        service = new CensoMatriculaImportService(parser, importacaoRepo, matriculaRepo, escolaRepo, tenant, SALT);
    }

    @Test
    @DisplayName("Import filtra município/dependência/etapa e persiste 8 das 12 linhas em batch")
    void import_filtra_e_persiste() throws Exception {
        var resultado = service.importar(fixture(), "MATRICULA_2024.csv", 2024, false, "admin_test");

        assertThat(resultado.registrosProcessados()).isEqualTo(12);
        assertThat(resultado.registrosMunicipio()).isEqualTo(8);
        assertThat(resultado.matriculasTotal()).isEqualTo(8);
        assertThat(resultado.escolasIgnoradasNaoMunicipal()).isEqualTo(2);
        assertThat(resultado.escolasIgnoradasInativas()).isEqualTo(1); // reusamos o slot para "etapa não mapeada"
        assertThat(resultado.dryRun()).isFalse();
        assertThat(resultado.status()).isEqualTo("concluida");

        // Persistiu via saveAll (batch). Como são só 8 linhas e batch=1000, 1 chamada.
        verify(matriculaRepo, atLeastOnce()).saveAll(any());
    }

    @Test
    @DisplayName("Resumo agrega por etapa: CRECHE=2, EF1=3, EF2=1, EM=1, EJA=1")
    void resumo_por_etapa() throws Exception {
        var resultado = service.importar(fixture(), "MATRICULA_2024.csv", 2024, false, "admin_test");

        var mapa = new java.util.HashMap<String, Integer>();
        for (var r : resultado.resumoMatriculas()) {
            mapa.put(r.etapaCodigo(), r.qtdAlunos());
        }
        assertThat(mapa).containsEntry("CRECHE", 2)
                        .containsEntry("EF1",    3)
                        .containsEntry("EF2",    1)
                        .containsEntry("EM",     1)
                        .containsEntry("EJA",    1);
    }

    @Test
    @DisplayName("ID_ALUNO é pseudonimizado via SHA-256(id || salt) — nunca persistido em claro")
    void id_aluno_pseudonimizado() throws Exception {
        ArgumentCaptor<List<CensoMatricula>> cap = ArgumentCaptor.forClass(List.class);
        service.importar(fixture(), "MATRICULA_2024.csv", 2024, false, "admin_test");
        verify(matriculaRepo).saveAll(cap.capture());

        var lista = cap.getValue();
        assertThat(lista).hasSize(8);
        for (var m : lista) {
            assertThat(m.getIdAlunoHash()).hasSize(64).matches("[0-9a-f]{64}");
            // Sanity: nenhum hash igual a um ID em claro
            assertThat(m.getIdAlunoHash()).doesNotContain("ALUNO-");
        }
        // Hash do mesmo ID com mesmo salt é estável
        String hashEsperado = service.pseudonimizar("ALUNO-A");
        assertThat(lista.stream().map(CensoMatricula::getIdAlunoHash))
                .contains(hashEsperado);
    }

    @Test
    @DisplayName("Pseudonimização muda quando salt muda (rotação de incidente LGPD)")
    void salt_diferente_gera_hash_diferente() {
        var tenant = new TenantProperties("172100", "Palmas", "1721000");
        var svc1 = new CensoMatriculaImportService(parser, importacaoRepo, matriculaRepo, escolaRepo, tenant, "salt-um-12345");
        var svc2 = new CensoMatriculaImportService(parser, importacaoRepo, matriculaRepo, escolaRepo, tenant, "salt-dois-12345");
        assertThat(svc1.pseudonimizar("ALUNO-A"))
                .isNotEqualTo(svc2.pseudonimizar("ALUNO-A"));
    }

    @Test
    @DisplayName("NEE flags são extraídas e codificadas em CSV (deficiência intelectual + autismo)")
    void nee_flags_csv() throws Exception {
        ArgumentCaptor<List<CensoMatricula>> cap = ArgumentCaptor.forClass(List.class);
        service.importar(fixture(), "MATRICULA_2024.csv", 2024, false, "admin_test");
        verify(matriculaRepo).saveAll(cap.capture());

        var lista = cap.getValue();
        var comNee = lista.stream().filter(CensoMatricula::getInNecessidadeEspecial).toList();
        assertThat(comNee).hasSize(2);
        assertThat(comNee).extracting(CensoMatricula::getNecessidadesCodigos)
                .containsExactlyInAnyOrder("DEFICIENCIA_INTELECTUAL", "AUTISMO");
    }

    @Test
    @DisplayName("Dry-run computa estatísticas mas não persiste nada")
    void dry_run_nao_persiste() throws Exception {
        var resultado = service.importar(fixture(), "MATRICULA_2024.csv", 2024, true, "gestor_test");

        assertThat(resultado.dryRun()).isTrue();
        assertThat(resultado.status()).isEqualTo("simulada");
        assertThat(resultado.matriculasTotal()).isEqualTo(8);
        assertThat(resultado.importacaoId()).isNull();

        verify(matriculaRepo, never()).saveAll(any());
        verify(importacaoRepo, never()).save(any());
        verify(importacaoRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Salt curto (<8 chars) é rejeitado por compliance LGPD")
    void salt_curto_rejeitado() {
        var tenant = new TenantProperties("172100", "Palmas", "1721000");
        var svc = new CensoMatriculaImportService(parser, importacaoRepo, matriculaRepo, escolaRepo, tenant, "abc");
        assertThatThrownBy(() -> svc.importar(fixture(), "x.csv", 2024, true, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pseudonimizacao-salt");
    }

    @Test
    @DisplayName("Cod-IBGE inválido bloqueia o import")
    void cod_ibge_invalido_bloqueia() {
        var bad = new TenantProperties("000000", "Sem", "0000000");
        var svc = new CensoMatriculaImportService(parser, importacaoRepo, matriculaRepo, escolaRepo, bad, SALT);
        assertThatThrownBy(() -> svc.importar(fixture(), "x.csv", 2024, true, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cod-municipio-ibge");
    }

    @Test
    @DisplayName("Etapa 65 (EJA) é mapeada corretamente; 77 não é mapeada")
    void mapeamento_etapas() {
        assertThat(CensoEtapaEnsinoMapper.mapear(1)).isEqualTo("CRECHE");
        assertThat(CensoEtapaEnsinoMapper.mapear(2)).isEqualTo("PRE");
        assertThat(CensoEtapaEnsinoMapper.mapear(4)).isEqualTo("EF1");
        assertThat(CensoEtapaEnsinoMapper.mapear(9)).isEqualTo("EF2");
        assertThat(CensoEtapaEnsinoMapper.mapear(25)).isEqualTo("EM");
        assertThat(CensoEtapaEnsinoMapper.mapear(65)).isEqualTo("EJA");
        assertThat(CensoEtapaEnsinoMapper.mapear(39)).isEqualTo("PROF");
        assertThat(CensoEtapaEnsinoMapper.mapear(77)).isNull();
        assertThat(CensoEtapaEnsinoMapper.mapear(null)).isNull();
    }

    private InputStream fixture() {
        var s = getClass().getResourceAsStream("/censo/MATRICULA_FIXTURE.csv");
        if (s == null) throw new IllegalStateException("Fixture /censo/MATRICULA_FIXTURE.csv não encontrado");
        return s;
    }
}
