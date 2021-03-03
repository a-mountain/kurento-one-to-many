package com.example.server;

import com.example.server.lib.JsonMessage;
import com.example.server.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class Experiments {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Hello implements Message {
        private String hello;
    }

    @Test
    @SneakyThrows
    public void test() {
        var objectMapper = new ObjectMapper();
        Message sdpasnwer = new Hello("sdpasnwer");
        var jsonNode = objectMapper.convertValue(sdpasnwer, JsonNode.class);
        var jsonMessage = new JsonMessage(sdpasnwer.getClass().getSimpleName(), jsonNode);
        System.out.println(sdpasnwer.getClass().getSimpleName());
        System.out.println(objectMapper.writeValueAsString(jsonMessage));
    }
}
