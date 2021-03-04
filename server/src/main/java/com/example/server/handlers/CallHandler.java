package com.example.server.handlers;

import com.example.server.lib.DispatcherHandler;
import com.example.server.lib.MessageDispatcher;
import com.example.server.lib.annotations.MessageHandler;
import com.example.server.lib.annotations.WebsocketHandler;
import com.example.server.messages.bi.IceCandidateMessage;
import com.example.server.messages.bi.StopCommunicationMessage;
import com.example.server.messages.in.StartStreamMessage;
import com.example.server.messages.in.StartWatchMessage;
import com.example.server.services.KurentoService;
import com.example.server.services.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Nonnull;

@Slf4j
@WebsocketHandler
public class CallHandler extends DispatcherHandler {

    private final KurentoService kurentoService;
    private final MessagingService messagingService;

    @Autowired
    public CallHandler(KurentoService kurentoService, MessagingService messagingService, MessageDispatcher dispatcher) {
        super(dispatcher);
        this.kurentoService = kurentoService;
        this.messagingService = messagingService;
    }

    @MessageHandler
    private void handleIceCandidate(IceCandidateMessage message, WebSocketSession session) {
        kurentoService.handleIceCandidate(session.getId(), message);
    }

    @MessageHandler
    private void startWatch(StartWatchMessage message, WebSocketSession session) {
        kurentoService.startWatch(session.getId(), message);
    }

    @MessageHandler
    private void startStream(StartStreamMessage message, WebSocketSession session) {
        kurentoService.startStreaming(session.getId(), message);
    }

    @MessageHandler
    private void stopCommunication(StopCommunicationMessage message, WebSocketSession session) {
        kurentoService.stop(session.getId());
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
        messagingService.removeWebSocketSession(session);
        kurentoService.stop(session.getId());
    }

    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession session) {
        messagingService.addWebSocketSession(session);
    }
}
