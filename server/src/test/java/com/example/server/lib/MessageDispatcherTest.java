package com.example.server.lib;

import com.example.server.messages.Message;
import com.example.server.services.MessageConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MessageDispatcherTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestMessage implements Message {
        private String body;
    }

    @SneakyThrows
    @Test
    public void dispatcher() {
        var objectMapper = new ObjectMapper();
        var message = new TestMessage("test");
        var jsonMessage = new JsonMessage(message.getClass().getSimpleName(), objectMapper.convertValue(message, JsonNode.class));
        var messageDispatcher = new MessageDispatcher(new MessageConverter());
        messageDispatcher.addHandler(TestMessage.class, this::handler);
        var sessionMock = mock(WebSocketSession.class);
        var json = objectMapper.writeValueAsString(jsonMessage);
        System.out.println(json);
        var textMessage = new TextMessage(json);
        messageDispatcher.dispatch(textMessage, sessionMock);
    }

    private void handler(TestMessage message, WebSocketSession session) {
        assertEquals(message.getBody(), "test");
    }
}
