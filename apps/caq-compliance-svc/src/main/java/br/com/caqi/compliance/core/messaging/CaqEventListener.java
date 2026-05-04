package br.com.caqi.compliance.core.messaging;

import br.com.caqi.compliance.api.dto.CalculoExecutadoEventDto;
import br.com.caqi.compliance.domain.AuditChainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome o evento de cálculo CAQ executado publicado por caq-engine-svc.
 * Por enquanto, só registra o evento na cadeia imutável de auditoria
 * (log_auditoria). A avaliação propriamente dita é triggered pelo POST
 * /api/v1/compliance/avaliar — manter a evaluation idempotente e
 * disparada por ação humana evita ruído nos primeiros pilotos.
 *
 * TODO Fase 3.C: opcionalmente disparar avaliarAno() no consumo, com debounce.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CaqEventListener {

    private final AuditChainService auditChain;

    @RabbitListener(queues = "caqi.compliance.calculo-executado.${caqi.tenant.municipio-id:000000}",
            id = "calculoExecutadoListener")
    public void onCalculoExecutado(CalculoExecutadoEventDto event) {
        log.info("Evento calculo.executado recebido: id={} calculo={} escola={} etapa={} ano={}",
                event.eventId(), event.calculoId(), event.escolaId(), event.etapaCodigo(), event.ano());

        auditChain.registrar(
                "calculo_caq",
                String.valueOf(event.calculoId()),
                "EVENTO_RECEBIDO",
                null
        );
    }
}
