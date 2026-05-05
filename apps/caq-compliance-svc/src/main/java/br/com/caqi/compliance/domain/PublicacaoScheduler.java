package br.com.caqi.compliance.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trigger periódico do PublicacaoService para cumprir o art. 48-A da LRF
 * (transparência ativa em até 24h após executado).
 *
 * Ativável via {@code caqi.publicacao.enabled=false} (testes, dev local).
 * Cron configurável via {@code caqi.publicacao.cron} — default a cada 6h.
 */
@Component
@ConditionalOnProperty(prefix = "caqi.publicacao", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PublicacaoScheduler {

    private final PublicacaoService service;

    @Scheduled(cron = "${caqi.publicacao.cron:0 0 */6 * * *}", zone = "America/Sao_Paulo")
    public void executar() {
        log.info("[scheduler] Disparando publicação automática LRF art. 48-A");
        try {
            var resultado = service.executarTudo(null, "scheduler");
            log.info("[scheduler] Publicação concluída: {} publicadas, {} skipped, {} falhas",
                    resultado.publicadas(), resultado.skipped(), resultado.falhas());
        } catch (Exception e) {
            log.error("[scheduler] Falha global na publicação automática", e);
        }
    }
}
