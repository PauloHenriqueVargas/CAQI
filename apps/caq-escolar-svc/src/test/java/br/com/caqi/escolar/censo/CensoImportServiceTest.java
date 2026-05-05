package br.com.caqi.escolar.censo;

import br.com.caqi.escolar.core.TenantProperties;
import br.com.caqi.escolar.domain.entity.CensoImportacao;
import br.com.caqi.escolar.domain.entity.CensoMatriculaResumo;
import br.com.caqi.escolar.domain.entity.Escola;
import br.com.caqi.escolar.domain.repo.CensoImportacaoRepository;
import br.com.caqi.escolar.domain.repo.CensoMatriculaResumoRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test do CensoImportService usando o fixture ESCOLAS_FIXTURE.csv (8 linhas).
 *
 * Cenário esperado para tenant configurado em Palmas/TO (1721000):
 *  - 8 registros processados
 *  - 4 escolas municipais ATIVAS em Palmas (17000001/2/3/8)
 *  - 2 ignoradas por dependência ≠ municipal (estadual + privada)
 *  - 1 ignorada por situação extinta
 *  - 1 ignorada silenciosamente por município ≠ alvo (Santa Maria/1722000)
 *  - Matrículas totais (CRECHE+PRE+EF1+EF2+EJA): 185 + 350 + 500 + 410 + 15 = 1460
 */
@DisplayName("CensoImportService — orquestração: filtro município + dependência + atividade")
class CensoImportServiceTest {

    private CensoEscolarParser parser;
    private CensoImportacaoRepository importacaoRepo;
    private CensoMatriculaResumoRepository resumoRepo;
    private EscolaRepository escolaRepo;
    private CensoImportService service;

    @BeforeEach
    void setUp() {
        parser = new CensoEscolarParser();
        importacaoRepo = mock(CensoImportacaoRepository.class);
        resumoRepo = mock(CensoMatriculaResumoRepository.class);
        escolaRepo = mock(EscolaRepository.class);
        TenantProperties tenant = new TenantProperties("172100", "Palmas", "1721000");
        service = new CensoImportService(parser, importacaoRepo, resumoRepo, escolaRepo, tenant);

        // Atribui ID incremental simulando IDENTITY do Postgres
        AtomicLong idGen = new AtomicLong(0);
        when(importacaoRepo.saveAndFlush(any())).thenAnswer(inv -> {
            CensoImportacao imp = inv.getArgument(0);
            imp.setId(idGen.incrementAndGet() + 1000);
            return imp;
        });
        when(importacaoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(escolaRepo.findByInepId(any())).thenReturn(Optional.empty());
        when(escolaRepo.save(any())).thenAnswer(inv -> {
            Escola e = inv.getArgument(0);
            e.setId(idGen.incrementAndGet());
            return e;
        });
        when(importacaoRepo.findByAnoCensoAndArquivoHashSha256AndCodMunicipioIbge(
                any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Import completo do fixture: 4 escolas municipais ativas, 1460 matrículas")
    void import_filtra_e_persiste_corretamente() throws Exception {
        var resultado = service.importar(fixture(), "ESCOLAS_2024.csv", 2024, false, "admin_test");

        assertThat(resultado.registrosProcessados()).isEqualTo(8);
        assertThat(resultado.registrosMunicipio()).isEqualTo(4);
        assertThat(resultado.escolasInseridas()).isEqualTo(4);
        assertThat(resultado.escolasAtualizadas()).isZero();
        assertThat(resultado.escolasIgnoradasNaoMunicipal()).isEqualTo(2);
        assertThat(resultado.escolasIgnoradasInativas()).isEqualTo(1);
        assertThat(resultado.matriculasTotal()).isEqualTo(1460);
        assertThat(resultado.dryRun()).isFalse();
        assertThat(resultado.status()).isEqualTo("concluida");
        assertThat(resultado.arquivoHashSha256()).hasSize(64);

        verify(escolaRepo, times(4)).save(any());
        // 9 inserts: CRECHE×2, PRE×3, EF1×2, EF2×2, EJA×1 = 10 (mas EMEI 17000001 não tem EJA)
        // CRECHE: 17000001 (120) + 17000008 (65) = 2 inserts
        // PRE:    17000001 (180) + 17000003 (60) + 17000008 (110) = 3 inserts
        // EF1:    17000002 (380) + 17000003 (120) = 2 inserts
        // EF2:    17000002 (320) + 17000003 (90) = 2 inserts
        // EJA:    17000002 (15) = 1 insert
        verify(resumoRepo, times(10)).save(any());
    }

    @Test
    @DisplayName("Resumo por etapa agregado: CRECHE=185, PRE=350, EF1=500, EF2=410, EJA=15")
    void resumo_agregado_por_etapa() throws Exception {
        var resultado = service.importar(fixture(), "ESCOLAS_2024.csv", 2024, false, "admin_test");

        var resumo = resultado.resumoMatriculas();
        assertThat(resumo).extracting("etapaCodigo", "qtdAlunos", "qtdEscolas")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("CRECHE", 185, 2),
                        org.assertj.core.groups.Tuple.tuple("PRE",    350, 3),
                        org.assertj.core.groups.Tuple.tuple("EF1",    500, 2),
                        org.assertj.core.groups.Tuple.tuple("EF2",    410, 2),
                        org.assertj.core.groups.Tuple.tuple("EJA",     15, 1));
    }

    @Test
    @DisplayName("Dry-run: parse + filtro funcionam mas nada é persistido")
    void dry_run_nao_persiste() throws Exception {
        var resultado = service.importar(fixture(), "ESCOLAS_2024.csv", 2024, true, "gestor_test");

        assertThat(resultado.dryRun()).isTrue();
        assertThat(resultado.status()).isEqualTo("simulada");
        assertThat(resultado.registrosMunicipio()).isEqualTo(4);
        assertThat(resultado.matriculasTotal()).isEqualTo(1460);
        assertThat(resultado.importacaoId()).isNull();

        verify(importacaoRepo, never()).save(any());
        verify(importacaoRepo, never()).saveAndFlush(any());
        verify(escolaRepo, never()).save(any());
        verify(resumoRepo, never()).save(any());
    }

    @Test
    @DisplayName("Idempotência: reimport do mesmo arquivo concluído retorna resultado anterior, sem reescrever")
    void idempotencia_hash_duplicado() throws Exception {
        // Pré-calcula hash do fixture
        var hashRes = parser.parse(fixture(), r -> {});
        var hashSha = hashRes.hashSha256Hex();

        var anterior = new CensoImportacao();
        anterior.setId(999L);
        anterior.setAnoCenso(2024);
        anterior.setArquivoHashSha256(hashSha);
        anterior.setArquivoNome("ESCOLAS_2024.csv");
        anterior.setCodMunicipioIbge("1721000");
        anterior.setStatus(CensoImportacao.Status.concluida);
        anterior.setRegistrosProcessados(8);
        anterior.setRegistrosMunicipio(4);
        anterior.setEscolasInseridas(4);
        anterior.setEscolasAtualizadas(0);
        anterior.setMatriculasTotal(1460);
        anterior.setCriadoPor("admin_test");
        anterior.setCriadoEm(java.time.LocalDateTime.now());

        when(importacaoRepo.findByAnoCensoAndArquivoHashSha256AndCodMunicipioIbge(
                2024, hashSha, "1721000")).thenReturn(Optional.of(anterior));
        when(resumoRepo.findByImportacaoId(999L)).thenReturn(List.of(
                criarResumo(999L, 2024, "17000001", "CRECHE", 120),
                criarResumo(999L, 2024, "17000008", "CRECHE",  65)
        ));

        var resultado = service.importar(fixture(), "ESCOLAS_2024.csv", 2024, false, "admin_test");
        assertThat(resultado.importacaoId()).isEqualTo(999L);
        assertThat(resultado.matriculasTotal()).isEqualTo(1460);
        verify(importacaoRepo, never()).save(any());
        verify(escolaRepo, never()).save(any());
        verify(resumoRepo, never()).save(any());
    }

    @Test
    @DisplayName("Escola já existente é atualizada, não duplicada (upsert por inep_id)")
    void escola_existente_atualizada() throws Exception {
        Escola castroAlves = new Escola();
        castroAlves.setId(42L);
        castroAlves.setInepId("17000002");
        castroAlves.setNome("Nome antigo");
        when(escolaRepo.findByInepId("17000002")).thenReturn(Optional.of(castroAlves));

        var resultado = service.importar(fixture(), "ESCOLAS_2024.csv", 2024, false, "admin_test");

        assertThat(resultado.escolasInseridas()).isEqualTo(3);
        assertThat(resultado.escolasAtualizadas()).isEqualTo(1);

        ArgumentCaptor<Escola> escolaCaptor = ArgumentCaptor.forClass(Escola.class);
        verify(escolaRepo, times(3)).save(escolaCaptor.capture());
        // A escola atualizada NÃO é salva separadamente — o ciclo @Transactional faz dirty checking;
        // mas seu nome foi atualizado em memória.
        assertThat(castroAlves.getNome()).isEqualTo("EMEF Castro Alves");
    }

    @Test
    @DisplayName("Cod-municipio-ibge inválido bloqueia o import com IllegalStateException")
    void cod_ibge_invalido_falha() {
        TenantProperties bad = new TenantProperties("000000", "Sem nome", "0000000");
        var svc = new CensoImportService(parser, importacaoRepo, resumoRepo, escolaRepo, bad);
        assertThatThrownBy(() -> svc.importar(fixture(), "x.csv", 2024, true, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cod-municipio-ibge");
    }

    @Test
    @DisplayName("Nenhum município bate com o filtro → registrosMunicipio=0, matriculas=0")
    void municipio_diferente_zera_filtrados() throws Exception {
        TenantProperties outro = new TenantProperties("172200", "Outro", "1722000");
        var svc = new CensoImportService(parser, importacaoRepo, resumoRepo, escolaRepo, outro);

        var resultado = svc.importar(fixture(), "ESCOLAS_2024.csv", 2024, false, "admin_test");
        assertThat(resultado.registrosMunicipio()).isEqualTo(1); // 17000006 EMEF Santa Maria
        assertThat(resultado.escolasInseridas()).isEqualTo(1);
        assertThat(resultado.matriculasTotal()).isEqualTo(180 + 160); // EF1 + EF2 do Santa Maria
    }

    private InputStream fixture() {
        var s = getClass().getResourceAsStream("/censo/ESCOLAS_FIXTURE.csv");
        if (s == null) throw new IllegalStateException("Fixture não encontrado");
        return s;
    }

    private CensoMatriculaResumo criarResumo(Long impId, Integer ano, String inep, String etapa, Integer qtd) {
        var r = new CensoMatriculaResumo();
        r.setImportacaoId(impId);
        r.setAnoCenso(ano);
        r.setEscolaInep(inep);
        r.setEtapaCodigo(etapa);
        r.setQtdAlunos(qtd);
        return r;
    }
}
