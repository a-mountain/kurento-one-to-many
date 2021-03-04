package com.example.server.lib;

import com.example.server.messages.Message;
import org.springframework.web.socket.WebSocketSession;

@FunctionalInterface
public interface MessageHandler<T extends Message> {
    void handleMessage(T message, WebSocketSession session);
}
