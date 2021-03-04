package com.example.server.lib;

import com.example.server.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class MessageConverter {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    @SneakyThrows
    private <T> String toJsonString(T obj) {
        return mapper.writeValueAsString(obj);
    }

    @SneakyThrows
    public JsonMessage convertToJsonMessage(String message) {
        return mapper.readValue(message, JsonMessage.class);
    }

    private JsonMessage convertToJsonMessage(Message message) {
        var type = message.getClass().getSimpleName();
        var json = mapper.convertValue(message, JsonNode.class);
        return new JsonMessage(type, json);
    }

    public String convertToJsonMessageAsText(Message message) {
        return toJsonString(convertToJsonMessage(message));
    }

    @SneakyThrows
    public Message getJsonMessageContent(JsonMessage message, Class<?> contentType) {
        var json = message.getContent();
        return (Message) mapper.readValue(json.traverse(), contentType);
    }
}
