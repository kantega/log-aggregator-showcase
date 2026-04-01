package no.kantega.edge.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "log-manager-exchange";
    public static final String QUEUE = "log-events-queue";
    public static final String ROUTING_KEY = "log.event";

    @Bean
    public TopicExchange logEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue edgeQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding binding(Queue edgeQueue, TopicExchange logEventsExchange) {
        return BindingBuilder.bind(edgeQueue).to(logEventsExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
