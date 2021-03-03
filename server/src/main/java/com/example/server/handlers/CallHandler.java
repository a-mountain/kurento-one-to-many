package com.example.server.handlers;

import com.example.server.services.MessagingService;
import com.example.server.lib.MessageDispatcher;
import com.example.server.messages.bi.IceCandidateMessage;
import com.example.server.messages.bi.StopCommunicationMessage;
import com.example.server.messages.in.StartStreamMessage;
import com.example.server.messages.in.StartWatchMessage;
import com.example.server.services.CallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Nonnull;

@Component
@Slf4j
public class CallHandler extends TextWebSocketHandler {

    private final CallService callService;
    private final MessagingService messagingService;
    private final MessageDispatcher dispatcher;

    @Autowired
    public CallHandler(CallService callService, MessagingService messagingService, MessageDispatcher dispatcher) {
        this.callService = callService;
        this.messagingService = messagingService;
        this.dispatcher = dispatcher;
        dispatcher
                .addHandler(IceCandidateMessage.class, this::handleIceCandidate)
                .addHandler(StartWatchMessage.class, this::startWatch)
                .addHandler(StartStreamMessage.class, this::startStream)
                .addHandler(StopCommunicationMessage.class, this::stopCommunication);
    }

    private void handleIceCandidate(IceCandidateMessage message, WebSocketSession session) {
        callService.handleIceCandidate(session.getId(), message);
    }

    private void startWatch(StartWatchMessage message, WebSocketSession session) {
        callService.startWatch(session.getId(), message);
    }

    private void startStream(StartStreamMessage message, WebSocketSession session) {
        callService.startStreaming(session.getId(), message);
    }

    private void stopCommunication(StopCommunicationMessage message, WebSocketSession session) {
        callService.stop(session.getId());
    }

    @Override
    protected void handleTextMessage(@Nonnull WebSocketSession session, @Nonnull TextMessage message) {
        String payload = message.getPayload();
        log.info("Incoming message: sessionId: {}, payload: {}", session.getId(), payload);
        dispatcher.dispatch(message, session);
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
        messagingService.removeWebSocketSession(session);
        callService.stop(session.getId());
    }

    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession session) {
        messagingService.addWebSocketSession(session);
    }
}
