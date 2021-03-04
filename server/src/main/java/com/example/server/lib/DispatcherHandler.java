package com.example.server.lib;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public class DispatcherHandler extends TextWebSocketHandler {

    private final MessageDispatcher dispatcher;

    public DispatcherHandler(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Incoming message: sessionId: {}, payload: {}", session.getId(), message.getPayload());
        dispatcher.dispatch(message, session);
    }
}
