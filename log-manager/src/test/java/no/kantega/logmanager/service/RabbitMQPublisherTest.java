package no.kantega.logmanager.service;

import no.kantega.logmanager.config.RabbitMQConfig;
import no.kantega.logmanager.model.LogGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQPublisher rabbitMQPublisher;

    private final LogGroup group = LogGroup.builder().id(1L).name("Test Group").status("OPEN").build();

    @SuppressWarnings("unchecked")
    @Test
    void entryAdded_messageContainsEntryId() {
        rabbitMQPublisher.publishEvent("ENTRY_ADDED", group, "log content", 42L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                captor.capture()
        );

        Map<String, Object> message = captor.getValue();
        assertEquals("ENTRY_ADDED", message.get("eventType"));
        assertEquals(1L, message.get("groupId"));
        assertEquals("Test Group", message.get("groupName"));
        assertEquals("log content", message.get("entryContent"));
        assertEquals(42L, message.get("entryId"));
        assertNotNull(message.get("timestamp"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void groupCreated_messageContainsAllFields() {
        rabbitMQPublisher.publishEvent("GROUP_CREATED", group, null, null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                captor.capture()
        );

        Map<String, Object> message = captor.getValue();
        assertEquals("GROUP_CREATED", message.get("eventType"));
        assertEquals(1L, message.get("groupId"));
        assertEquals("Test Group", message.get("groupName"));
        assertNull(message.get("entryContent"));
        assertNull(message.get("entryId"));
        assertNotNull(message.get("timestamp"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void groupClosed_messageContainsAllFields() {
        rabbitMQPublisher.publishEvent("GROUP_CLOSED", group, null, null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                captor.capture()
        );

        Map<String, Object> message = captor.getValue();
        assertEquals("GROUP_CLOSED", message.get("eventType"));
        assertEquals(1L, message.get("groupId"));
        assertNull(message.get("entryContent"));
        assertNull(message.get("entryId"));
    }
}
