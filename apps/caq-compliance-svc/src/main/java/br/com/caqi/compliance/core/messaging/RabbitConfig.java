package br.com.caqi.compliance.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EVENTS_EXCHANGE = "caqi.events";
    public static final String CALCULO_QUEUE_PREFIX = "caqi.compliance.calculo-executado";
    public static final String CALCULO_RK_PREFIX = "caqi.calculo.executado";

    @Bean
    public TopicExchange caqiEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    /**
     * Queue específica deste tenant (instância por município) para receber
     * o evento "calculo.executado". Idempotente — RabbitMQ cria se não existir.
     */
    @Bean
    public Queue calculoExecutadoQueue(@Value("${caqi.tenant.municipio-id:000000}") String municipioId) {
        return new Queue(CALCULO_QUEUE_PREFIX + "." + municipioId, true);
    }

    @Bean
    public Binding calculoExecutadoBinding(Queue calculoExecutadoQueue, TopicExchange caqiEventsExchange,
                                           @Value("${caqi.tenant.municipio-id:000000}") String municipioId) {
        return BindingBuilder.bind(calculoExecutadoQueue)
                .to(caqiEventsExchange)
                .with(CALCULO_RK_PREFIX + "." + municipioId);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(om);
    }
}
