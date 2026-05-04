package br.com.caqi.engine.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "caqi.events.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    /** Topic exchange usado por todos os eventos do domínio CAQ. */
    public static final String EVENTS_EXCHANGE = "caqi.events";

    /** Routing key base — sufixada com tenantMunicipioId no envio. */
    public static final String CALCULO_EXECUTADO_RK_PREFIX = "caqi.calculo.executado";

    @Bean
    public TopicExchange caqiEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(om);
    }
}
