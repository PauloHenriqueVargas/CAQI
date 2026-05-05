package br.com.caqi.escolar.censo;

import br.com.caqi.escolar.censo.dto.CensoImportResultDto;
import br.com.caqi.escolar.core.TenantProperties;
import br.com.caqi.escolar.domain.entity.CensoImportacao;
import br.com.caqi.escolar.domain.entity.CensoMatricula;
import br.com.caqi.escolar.domain.repo.CensoImportacaoRepository;
import br.com.caqi.escolar.domain.repo.CensoMatriculaRepository;
import br.com.caqi.escolar.domain.repo.EscolaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Importa microdados aluno-level (MATRICULA_*.csv) com pseudonimização do
 * ID_ALUNO, filtragem por município/dependência municipal, mapeamento de
 * etapa via {@link CensoEtapaEnsinoMapper}.
 *
 * <h3>LGPD</h3>
 * <ul>
 *   <li>ID_ALUNO INEP nunca é persistido em claro — só o SHA-256 com salt
 *       per tenant ({@code CAQI_CENSO_PSEUDONIMIZACAO_SALT}). Assim, o
 *       cruzamento com bases externas (que conhecem o ID_ALUNO) não é
 *       possível diretamente; um ataque exigiria conhecer o salt.</li>
 *   <li>Data de nascimento é descartada — guardamos apenas a idade
 *       agregada (NU_IDADE_REFERENCIA do INEP).</li>
 *   <li>Campos sensíveis (cor/raça, NEE) são código numérico INEP, sem
 *       texto livre, e só servem a marts agregados em
 *       {@code analytics_marts}, nunca expostos em /api/public/.</li>
 * </ul>
 *
 * <h3>Volume</h3>
 * Arquivo Brasil-inteiro tem ~50M linhas. Após filtro por município
 * tipicamente fica entre 10k e 500k. Persiste em batch de 1000 para
 * limitar uso de heap.
 */
@Service
@Slf4j
public class CensoMatriculaImportService {

    private static final int BATCH_SIZE = 1_000;

    private final CensoMatriculaParser parser;
    private final CensoImportacaoRepository importacaoRepo;
    private final CensoMatriculaRepository matriculaRepo;
    private final EscolaRepository escolaRepo;
    private final TenantProperties tenant;
    private final byte[] pseudonimizacaoSaltBytes;

    public CensoMatriculaImportService(
            CensoMatriculaParser parser,
            CensoImportacaoRepository importacaoRepo,
            CensoMatriculaRepository matriculaRepo,
            EscolaRepository escolaRepo,
            TenantProperties tenant,
            @Value("${caqi.censo.pseudonimizacao-salt:trocar_em_prod}") String salt
    ) {
        this.parser = parser;
        this.importacaoRepo = importacaoRepo;
        this.matriculaRepo = matriculaRepo;
        this.escolaRepo = escolaRepo;
        this.tenant = tenant;
        this.pseudonimizacaoSaltBytes = salt.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public CensoImportResultDto importar(
            InputStream stream,
            String arquivoNome,
            int anoCenso,
            boolean dryRun,
            String username
    ) throws IOException {
        validarConfig();
        String codIbge = tenant.codMunicipioIbge();
        log.info("Iniciando import MATRICULA: arquivo={} ano={} dryRun={} cod-ibge={} usuario={}",
                arquivoNome, anoCenso, dryRun, codIbge, username);

        // Cache de escola_id por inep para evitar N selects no for-loop.
        Map<String, Long> escolaIdCache = new HashMap<>();
        AtomicInteger ignoradasNaoMunicipal = new AtomicInteger();
        AtomicInteger ignoradasEtapaNaoMapeada = new AtomicInteger();

        // Acumulador de matrículas válidas (para contagem demográfica em memória)
        Map<String, Integer> contagemPorEtapa = new HashMap<>();
        AtomicInteger totalNee = new AtomicInteger();

        // Cria registro da importação cedo para ter ID na hora de inserir matrículas
        CensoImportacao importacao = new CensoImportacao();
        importacao.setAnoCenso(anoCenso);
        importacao.setArquivoNome(arquivoNome);
        importacao.setArquivoHashSha256("pendente");
        importacao.setCodMunicipioIbge(codIbge);
        importacao.setStatus(dryRun ? CensoImportacao.Status.simulada : CensoImportacao.Status.em_andamento);
        importacao.setCriadoPor(username);
        importacao.setCriadoEm(LocalDateTime.now());

        if (!dryRun) {
            importacao = importacaoRepo.saveAndFlush(importacao);
        }

        final Long importacaoId = dryRun ? null : importacao.getId();
        final List<CensoMatricula> batch = new ArrayList<>(BATCH_SIZE);
        AtomicInteger inseridas = new AtomicInteger();

        var resultado = parser.parse(stream, rec -> {
            if (rec == null || rec.coMunicipio() == null) return;
            if (!codIbge.equals(rec.coMunicipio().trim())) return;
            if (!rec.ehDependenciaMunicipal()) {
                ignoradasNaoMunicipal.incrementAndGet();
                return;
            }
            String etapa = CensoEtapaEnsinoMapper.mapear(rec.tpEtapaEnsino());
            if (etapa == null) {
                ignoradasEtapaNaoMapeada.incrementAndGet();
                return;
            }

            if (rec.idMatricula() == null || rec.idMatricula().isBlank()) return;
            if (rec.idAluno() == null || rec.idAluno().isBlank()) return;
            if (rec.coEntidade() == null || rec.coEntidade().isBlank()) return;

            CensoMatricula m = new CensoMatricula();
            m.setImportacaoId(importacaoId);
            m.setAnoCenso(anoCenso);
            m.setIdMatriculaInep(rec.idMatricula().trim());
            m.setIdAlunoHash(pseudonimizar(rec.idAluno().trim()));
            m.setEscolaInep(rec.coEntidade().trim());
            m.setEscolaId(escolaIdCache.computeIfAbsent(rec.coEntidade().trim(),
                    inep -> escolaRepo.findByInepId(inep).map(e -> e.getId()).orElse(null)));
            m.setEtapaCodigo(etapa);
            m.setTpEtapaEnsino(rec.tpEtapaEnsino() != null ? rec.tpEtapaEnsino().shortValue() : null);
            m.setIdade(rec.nuIdadeReferencia() != null ? rec.nuIdadeReferencia().shortValue() : null);
            m.setTpSexo(rec.tpSexo() != null ? rec.tpSexo().shortValue() : null);
            m.setTpCorRaca(rec.tpCorRaca() != null ? rec.tpCorRaca().shortValue() : null);
            m.setTpZonaResidencial(rec.tpZonaResidencial() != null ? rec.tpZonaResidencial().shortValue() : null);
            m.setInNecessidadeEspecial(rec.inNecessidadeEspecialBool());
            m.setNecessidadesCodigos(rec.necessidadesCsv());

            contagemPorEtapa.merge(etapa, 1, Integer::sum);
            if (rec.inNecessidadeEspecialBool()) totalNee.incrementAndGet();

            if (!dryRun) {
                batch.add(m);
                if (batch.size() >= BATCH_SIZE) {
                    matriculaRepo.saveAll(batch);
                    inseridas.addAndGet(batch.size());
                    batch.clear();
                }
            } else {
                inseridas.incrementAndGet();
            }
        });

        // Flush final
        if (!dryRun && !batch.isEmpty()) {
            matriculaRepo.saveAll(batch);
            inseridas.addAndGet(batch.size());
            batch.clear();
        }

        // Atualiza importacao com hash final + contadores
        importacao.setArquivoHashSha256(resultado.hashSha256Hex());
        importacao.setArquivoTamanhoBytes(resultado.bytesLidos());
        importacao.setRegistrosProcessados(resultado.linhas());
        importacao.setRegistrosMunicipio(inseridas.get());
        importacao.setMatriculasTotal(inseridas.get());
        importacao.setMatriculasInseridas(dryRun ? 0 : inseridas.get());
        importacao.setSubtipoArquivo("MATRICULA");
        importacao.setStatus(dryRun ? CensoImportacao.Status.simulada : CensoImportacao.Status.concluida);
        if (!dryRun) {
            importacaoRepo.save(importacao);
        }

        log.info("Import MATRICULA {}: processadas={} municipal={} ignoradas-dep={} ignoradas-etapa={} NEE={} dryRun={}",
                anoCenso, resultado.linhas(), inseridas.get(),
                ignoradasNaoMunicipal.get(), ignoradasEtapaNaoMapeada.get(), totalNee.get(), dryRun);

        var resumoList = new ArrayList<CensoImportResultDto.MatriculaResumoDto>();
        for (var entry : contagemPorEtapa.entrySet()) {
            resumoList.add(new CensoImportResultDto.MatriculaResumoDto(entry.getKey(), entry.getValue(), null));
        }

        return new CensoImportResultDto(
                dryRun ? null : importacao.getId(),
                anoCenso,
                codIbge,
                arquivoNome,
                resultado.hashSha256Hex(),
                resultado.bytesLidos(),
                resultado.linhas(),
                inseridas.get(),
                0,                            // escolasInseridas (não cria escolas neste fluxo)
                0,                            // escolasAtualizadas
                inseridas.get(),              // matriculasTotal = aluno-level rows
                ignoradasNaoMunicipal.get(),
                ignoradasEtapaNaoMapeada.get(),
                importacao.getStatus().name(),
                importacao.getErroMensagem(),
                importacao.getCriadoEm(),
                username,
                dryRun,
                resumoList
        );
    }

    /**
     * SHA-256(id_aluno || salt) em hex. Salt vem de env var por tenant —
     * em prod, gerado por DPO + rotacionado em incidente LGPD.
     */
    String pseudonimizar(String idAluno) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(idAluno.getBytes(StandardCharsets.UTF_8));
            md.update(pseudonimizacaoSaltBytes);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private void validarConfig() {
        String cod = tenant.codMunicipioIbge();
        if (cod == null || cod.length() != 7 || !cod.chars().allMatch(Character::isDigit) || cod.equals("0000000")) {
            throw new IllegalStateException(
                    "caqi.tenant.cod-municipio-ibge inválido: configure o IBGE 7-dígitos.");
        }
        if (pseudonimizacaoSaltBytes == null || pseudonimizacaoSaltBytes.length < 8) {
            throw new IllegalStateException(
                    "caqi.censo.pseudonimizacao-salt deve ter ao menos 8 bytes (LGPD).");
        }
    }
}
