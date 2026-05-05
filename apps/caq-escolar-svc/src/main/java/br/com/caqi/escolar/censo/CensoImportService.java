package br.com.caqi.escolar.censo;

import br.com.caqi.escolar.censo.dto.CensoImportResultDto;
import br.com.caqi.escolar.core.TenantProperties;
import br.com.caqi.escolar.domain.entity.CensoImportacao;
import br.com.caqi.escolar.domain.entity.CensoMatriculaResumo;
import br.com.caqi.escolar.domain.entity.Escola;
import br.com.caqi.escolar.domain.repo.CensoImportacaoRepository;
import br.com.caqi.escolar.domain.repo.CensoMatriculaResumoRepository;
import br.com.caqi.escolar.domain.repo.EscolaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Importa microdados Censo Escolar/INEP — arquivo ESCOLAS_*.csv.
 *
 * Pipeline:
 *  1. Calcula SHA-256 + parse streaming Latin-1 / ';'
 *  2. Verifica idempotência (ano_censo + hash + cod_municipio): se já
 *     importado e status='concluida', retorna o resultado anterior
 *  3. Filtra por CO_MUNICIPIO = tenant.cod-municipio-ibge
 *  4. Filtra por TP_DEPENDENCIA = 3 (Municipal) e TP_SITUACAO = 1 (em atividade)
 *  5. Upsert escola pelo natural key inep_id (CO_ENTIDADE)
 *  6. Insere CensoMatriculaResumo para cada (escola, etapa) com count > 0
 *  7. Atualiza censo_importacao com contadores finais
 *
 * Modo dry-run: parse + filtro acontecem normalmente, mas nada é persistido.
 *
 * Anomalias possíveis (registradas em log mas não interrompem):
 *  - escola sem CO_ENTIDADE (linha ignorada no contador registrosMunicipio)
 *  - quantidades negativas (clamped para 0)
 *  - tp_dependencia/tp_situacao nulos (escola ignorada)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CensoImportService {

    private final CensoEscolarParser parser;
    private final CensoImportacaoRepository importacaoRepo;
    private final CensoMatriculaResumoRepository resumoRepo;
    private final EscolaRepository escolaRepo;
    private final TenantProperties tenant;

    @Transactional
    public CensoImportResultDto importar(
            InputStream stream,
            String arquivoNome,
            int anoCenso,
            boolean dryRun,
            String username
    ) throws IOException {
        validarConfigTenant();

        String codIbge = tenant.codMunicipioIbge();
        log.info("Iniciando import Censo: arquivo={} ano={} dryRun={} cod-ibge={} usuario={}",
                arquivoNome, anoCenso, dryRun, codIbge, username);

        // Acumuladores em memória — reservados (com filtros aplicados in-stream)
        List<CensoEscolaRecord> filtrados = new ArrayList<>();
        AtomicInteger ignoradasNaoMunicipal = new AtomicInteger();
        AtomicInteger ignoradasInativas = new AtomicInteger();

        var resultadoParse = parser.parse(stream, rec -> {
            if (rec == null || rec.coMunicipio() == null) return;
            if (!codIbge.equals(rec.coMunicipio().trim())) return;
            if (!rec.ehMunicipal()) {
                ignoradasNaoMunicipal.incrementAndGet();
                return;
            }
            if (!rec.emAtividade()) {
                ignoradasInativas.incrementAndGet();
                return;
            }
            filtrados.add(rec);
        });

        // Idempotência: re-import do mesmo arquivo no mesmo município/ano não duplica
        var existente = importacaoRepo.findByAnoCensoAndArquivoHashSha256AndCodMunicipioIbge(
                anoCenso, resultadoParse.hashSha256Hex(), codIbge);
        if (existente.isPresent() && existente.get().getStatus() == CensoImportacao.Status.concluida && !dryRun) {
            log.info("Censo já importado anteriormente — retornando resultado existente (id={})",
                    existente.get().getId());
            return montarResultado(existente.get(), resultadoParse, filtrados.size(),
                    ignoradasNaoMunicipal.get(), ignoradasInativas.get(), false);
        }

        // Cria registro de importação
        CensoImportacao importacao = new CensoImportacao();
        importacao.setAnoCenso(anoCenso);
        importacao.setArquivoNome(arquivoNome);
        importacao.setArquivoHashSha256(resultadoParse.hashSha256Hex());
        importacao.setArquivoTamanhoBytes(resultadoParse.bytesLidos());
        importacao.setCodMunicipioIbge(codIbge);
        importacao.setRegistrosProcessados(resultadoParse.linhas());
        importacao.setRegistrosMunicipio(filtrados.size());
        importacao.setStatus(dryRun ? CensoImportacao.Status.simulada : CensoImportacao.Status.em_andamento);
        importacao.setCriadoEm(LocalDateTime.now());
        importacao.setCriadoPor(username);

        if (!dryRun) {
            importacao = importacaoRepo.saveAndFlush(importacao);
        }

        int inseridas = 0;
        int atualizadas = 0;
        int matriculasTotal = 0;
        Map<String, int[]> resumoPorEtapa = new HashMap<>();

        for (CensoEscolaRecord rec : filtrados) {
            if (rec.coEntidade() == null || rec.coEntidade().isBlank()) {
                log.debug("Ignorando registro sem CO_ENTIDADE: {}", rec.noEntidade());
                continue;
            }
            String inep = rec.coEntidade().trim();

            Escola escola;
            if (dryRun) {
                escola = escolaRepo.findByInepId(inep).orElse(null);
                if (escola == null) inseridas++;
                else atualizadas++;
            } else {
                var existenteEscola = escolaRepo.findByInepId(inep);
                if (existenteEscola.isPresent()) {
                    escola = existenteEscola.get();
                    atualizou(escola, rec);
                    atualizadas++;
                } else {
                    escola = new Escola();
                    escola.setInepId(inep);
                    atualizou(escola, rec);
                    escola = escolaRepo.save(escola);
                    inseridas++;
                }
            }

            for (var entry : CensoEtapaMapper.ETAPA_PARA_CAMPO.entrySet()) {
                String etapaCodigo = entry.getKey();
                Integer qtd = entry.getValue().apply(rec);
                if (qtd == null || qtd <= 0) continue;

                matriculasTotal += qtd;
                resumoPorEtapa.computeIfAbsent(etapaCodigo, k -> new int[]{0, 0});
                resumoPorEtapa.get(etapaCodigo)[0] += qtd;
                resumoPorEtapa.get(etapaCodigo)[1] += 1;

                if (!dryRun) {
                    CensoMatriculaResumo resumo = new CensoMatriculaResumo();
                    resumo.setImportacaoId(importacao.getId());
                    resumo.setAnoCenso(anoCenso);
                    resumo.setEscolaInep(inep);
                    resumo.setEscolaId(escola != null ? escola.getId() : null);
                    resumo.setEtapaCodigo(etapaCodigo);
                    resumo.setQtdAlunos(qtd);
                    resumoRepo.save(resumo);
                }
            }
        }

        importacao.setEscolasInseridas(inseridas);
        importacao.setEscolasAtualizadas(atualizadas);
        importacao.setMatriculasTotal(matriculasTotal);
        importacao.setStatus(dryRun ? CensoImportacao.Status.simulada : CensoImportacao.Status.concluida);

        if (!dryRun) {
            importacao = importacaoRepo.save(importacao);
        }

        var resumoList = new ArrayList<CensoImportResultDto.MatriculaResumoDto>(resumoPorEtapa.size());
        for (String etapaCodigo : CensoEtapaMapper.ETAPA_PARA_CAMPO.keySet()) {
            int[] tot = resumoPorEtapa.get(etapaCodigo);
            if (tot != null) {
                resumoList.add(new CensoImportResultDto.MatriculaResumoDto(etapaCodigo, tot[0], tot[1]));
            }
        }

        log.info("Import Censo {}: processados={} municipio={} inseridas={} atualizadas={} matriculas={} dryRun={}",
                anoCenso, resultadoParse.linhas(), filtrados.size(),
                inseridas, atualizadas, matriculasTotal, dryRun);

        return new CensoImportResultDto(
                dryRun ? null : importacao.getId(),
                anoCenso,
                codIbge,
                arquivoNome,
                resultadoParse.hashSha256Hex(),
                resultadoParse.bytesLidos(),
                resultadoParse.linhas(),
                filtrados.size(),
                inseridas,
                atualizadas,
                matriculasTotal,
                ignoradasNaoMunicipal.get(),
                ignoradasInativas.get(),
                importacao.getStatus().name(),
                importacao.getErroMensagem(),
                importacao.getCriadoEm(),
                username,
                dryRun,
                resumoList
        );
    }

    private CensoImportResultDto montarResultado(CensoImportacao existente,
                                                  CensoEscolarParser.ResultadoParse parse,
                                                  int registrosMunicipio,
                                                  int ignoradasNaoMunicipal,
                                                  int ignoradasInativas,
                                                  boolean dryRun) {
        var resumos = resumoRepo.findByImportacaoId(existente.getId());
        Map<String, int[]> agg = new HashMap<>();
        for (var r : resumos) {
            agg.computeIfAbsent(r.getEtapaCodigo(), k -> new int[]{0, 0});
            agg.get(r.getEtapaCodigo())[0] += r.getQtdAlunos();
            agg.get(r.getEtapaCodigo())[1] += 1;
        }
        var lista = CensoEtapaMapper.ETAPA_PARA_CAMPO.keySet().stream()
                .filter(agg::containsKey)
                .map(k -> new CensoImportResultDto.MatriculaResumoDto(k, agg.get(k)[0], agg.get(k)[1]))
                .toList();
        return new CensoImportResultDto(
                existente.getId(), existente.getAnoCenso(), existente.getCodMunicipioIbge(),
                existente.getArquivoNome(), existente.getArquivoHashSha256(),
                existente.getArquivoTamanhoBytes(),
                existente.getRegistrosProcessados(), existente.getRegistrosMunicipio(),
                existente.getEscolasInseridas(), existente.getEscolasAtualizadas(),
                existente.getMatriculasTotal(),
                ignoradasNaoMunicipal, ignoradasInativas,
                existente.getStatus().name(), existente.getErroMensagem(),
                existente.getCriadoEm(), existente.getCriadoPor(),
                dryRun, lista
        );
    }

    private void atualizou(Escola escola, CensoEscolaRecord rec) {
        if (rec.noEntidade() != null && !rec.noEntidade().isBlank()) {
            escola.setNome(rec.noEntidade().trim());
        } else if (escola.getNome() == null) {
            escola.setNome("Escola sem nome (INEP " + rec.coEntidade() + ")");
        }
        escola.setRede("municipal");
        String loc = rec.localizacaoTexto();
        if (loc != null) escola.setLocalizacao(loc);
        escola.setSituacao(rec.emAtividade() ? "ativa" : "inativa");
    }

    private void validarConfigTenant() {
        String cod = tenant.codMunicipioIbge();
        if (cod == null || cod.length() != 7 || !cod.chars().allMatch(Character::isDigit) || cod.equals("0000000")) {
            throw new IllegalStateException(
                    "caqi.tenant.cod-municipio-ibge inválido (atual='" + cod + "'): " +
                    "configure o IBGE 7-dígitos antes de importar Censo INEP. " +
                    "Ex.: Palmas/TO=1721000.");
        }
    }
}
