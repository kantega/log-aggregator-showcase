package no.kantega.logmanager.service;

import no.kantega.logmanager.config.RabbitMQConfig;
import no.kantega.logmanager.model.LogGroup;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishEvent(String eventType, LogGroup group, String entryContent) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", eventType);
        message.put("groupId", group.getId());
        message.put("groupName", group.getName());
        message.put("entryContent", entryContent);
        message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                message
        );
    }
}
