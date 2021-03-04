package com.example.server.lib;

import com.example.server.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MessageDispatcher {

    private final Map<String, TypedHandler<?>> handlers = new HashMap<>();
    private final MessageConverter converter;

    @Autowired
    public MessageDispatcher(MessageConverter converter) {
        this.converter = converter;
    }

    public <T extends Message> MessageDispatcher addHandler(Class<T> messageType, MessageHandler<T> handler) {
        var typedHandler = new TypedHandler<T>(messageType, handler);
        var path = messageType.getSimpleName();
        handlers.put(path, typedHandler);
        return this;
    }

    private void useHandler(JsonMessage jsonMessage, TypedHandler<?> typedHandler, WebSocketSession session) {
        var content = converter.getJsonMessageContent(jsonMessage, typedHandler.getMessageType());
        var handler = (MessageHandler<Message>) typedHandler.getHandler();
        handler.handleMessage(content, session);
    }

    public void dispatch(TextMessage message, WebSocketSession session) {
        var payload = message.getPayload();
        var jsonMessage = converter.convertToJsonMessage(payload);
        var type = jsonMessage.getType();
        if (handlers.containsKey(type)) {
            var typedHandler = handlers.get(type);
            useHandler(jsonMessage, typedHandler, session);
            return;
        }
        log.info("Unrecognized message: {}", payload);
    }
}
