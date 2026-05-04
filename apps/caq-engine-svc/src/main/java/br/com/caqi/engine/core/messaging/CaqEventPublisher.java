package br.com.caqi.engine.core.messaging;

import br.com.caqi.shared.events.CalculoExecutadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publica eventos no exchange caqi.events. Falhas são LOGADAS mas NÃO
 * propagam (fire-and-forget) — não é aceitável que indisponibilidade de
 * mensageria derrube a operação principal (cálculo). Para semântica
 * forte usar transactional outbox em fase posterior.
 */
@Component
@ConditionalOnProperty(name = "caqi.events.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CaqEventPublisher {

    private final RabbitTemplate rabbit;

    @Value("${caqi.tenant.municipio-id:000000}")
    private String municipioId;

    public void publicar(CalculoExecutadoEvent event) {
        String routingKey = RabbitConfig.CALCULO_EXECUTADO_RK_PREFIX + "." + municipioId;
        try {
            rabbit.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, routingKey, event);
            log.debug("Publicado {} → {}", event.eventId(), routingKey);
        } catch (AmqpException e) {
            log.error("Falha publicando evento {} (routingKey={}) — operação principal preservada",
                    event.eventId(), routingKey, e);
        }
    }
}
