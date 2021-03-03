package com.example.server.services;

import com.example.server.domain.UserSession;
import com.example.server.messages.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MessagingService {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final MessageConverter converter;

    public MessagingService(MessageConverter converter) {
        this.converter = converter;
    }

    public void addWebSocketSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void removeWebSocketSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public synchronized void sendMessage(String sessionId, Message message) {
        var session = sessions.get(sessionId);
        var json = converter.convertToJsonMessageAsText(message);
        log.info("Send message: {}", json);
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(UserSession session, Message message) {
        sendMessage(session.getSessionId(), message);
    }

}
