package br.com.caqi.compliance.domain;

import br.com.caqi.compliance.api.dto.ExecucaoFundebDto;
import br.com.caqi.compliance.core.client.FinanceiroClient;
import br.com.caqi.compliance.domain.entity.Notificacao;
import br.com.caqi.compliance.domain.repo.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aplica as regras legais de vinculação para um exercício e gera notificações
 * quando algum percentual mínimo é violado.
 *
 * Fonte de dados: caq-financeiro-svc via REST (FinanceiroClient). O cálculo
 * em si fica no produtor — aqui só interpreta + classifica + persiste.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvaliadorComplianceService {

    private static final String LEI_FUNDEB = "Lei 14.113/2020";
    private static final String CF_212     = "CF/88, art. 212";

    private final FinanceiroClient financeiroClient;
    private final NotificacaoRepository notificacaoRepo;
    private final AuditChainService auditChain;

    @Transactional
    public List<Notificacao> avaliarAno(int ano) {
        ExecucaoFundebDto exec = financeiroClient.execucao(ano);
        log.info("Avaliando ano={} → MDE={}% Fundeb70={}% VAAT15={}%",
                ano, exec.pctMde(), exec.pctFundebPessoal(), exec.pctVaatCapital());

        List<Notificacao> criadas = new ArrayList<>();

        if (Boolean.FALSE.equals(exec.cumpreMde())) {
            criadas.add(criar("VIOLACAO_MDE_25", "critica",
                    "MDE abaixo do mínimo constitucional (25%)",
                    String.format("Executado %s%%, mínimo 25%% (CF/88 art. 212).", exec.pctMde()),
                    ano, CF_212, payloadVinculacao(exec.pctMde(), new BigDecimal("25"), exec)));
        }
        if (Boolean.FALSE.equals(exec.cumpreFundebPessoal())) {
            criadas.add(criar("VIOLACAO_FUNDEB_70", "critica",
                    "Fundeb pessoal abaixo do mínimo de 70%",
                    String.format("Executado %s%%, mínimo 70%% (Lei 14.113/2020).", exec.pctFundebPessoal()),
                    ano, LEI_FUNDEB, payloadVinculacao(exec.pctFundebPessoal(), new BigDecimal("70"), exec)));
        }
        if (Boolean.FALSE.equals(exec.cumpreVaatCapital())) {
            criadas.add(criar("VIOLACAO_VAAT_15", "alta",
                    "VAAT capital abaixo do mínimo de 15%",
                    String.format("Executado %s%%, mínimo 15%% (Lei 14.113/2020).", exec.pctVaatCapital()),
                    ano, LEI_FUNDEB, payloadVinculacao(exec.pctVaatCapital(), new BigDecimal("15"), exec)));
        }

        if (criadas.isEmpty()) {
            log.info("Avaliação ano={}: tudo dentro dos limites legais — nenhuma notificação gerada", ano);
        }
        return criadas;
    }

    private Notificacao criar(String tipo, String severidade, String titulo, String descricao,
                              int ano, String baseLegal, Map<String, Object> payload) {
        Notificacao n = new Notificacao();
        n.setTipo(tipo);
        n.setSeveridade(severidade);
        n.setTitulo(titulo);
        n.setDescricao(descricao);
        n.setAnoReferencia(ano);
        n.setBaseLegal(baseLegal);
        n.setPayload(payload);
        n = notificacaoRepo.save(n);
        auditChain.registrar("notificacao", String.valueOf(n.getId()), "INSERT", null);
        return n;
    }

    private Map<String, Object> payloadVinculacao(BigDecimal executado, BigDecimal minimo, ExecucaoFundebDto exec) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("percentual_executado", executado);
        p.put("percentual_minimo", minimo);
        p.put("gap_pp", minimo.subtract(executado));
        p.put("ano", exec.ano());
        return p;
    }
}
