package com.example.server.handlers;

import com.example.server.ConverterService;
import com.example.server.domain.UserSession;
import com.example.server.messages.Message;
import com.example.server.messages.bi.StopCommunication;
import com.example.server.messages.in.IceCandidateOffer;
import com.example.server.messages.in.PresenterOfferMessage;
import com.example.server.messages.in.ViewerOfferMessage;
import com.example.server.messages.out.*;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CallHandler extends TextWebSocketHandler {

    private final ConverterService converterService;

    private final Map<String, UserSession> viewers = new ConcurrentHashMap<>();

    private final KurentoClient kurento;

    private MediaPipeline pipeline;
    private UserSession presenterUserSession;

    @Autowired
    public CallHandler(ConverterService converterService, KurentoClient kurento) {
        this.converterService = converterService;
        this.kurento = kurento;
    }

    @Override
    protected void handleTextMessage(@Nonnull WebSocketSession session, @Nonnull TextMessage message) {
        String payload = message.getPayload();
        log.info("Incoming message: sessionId: {}, payload: {}", session.getId(), payload);
        var msg = converterService.toEntity(payload, Message.class);
        var messageClass = msg.getClass();
        if (messageClass.equals(PresenterOfferMessage.class)) {
            presenter(session, (PresenterOfferMessage) msg);
            return;
        }
        if (messageClass.equals(ViewerOfferMessage.class)) {
            viewer(session, (ViewerOfferMessage) msg);
        }
        if (messageClass.equals(IceCandidateOffer.class)) {
            handleIceCandidate(session, (IceCandidateOffer) msg);
        }
        if (messageClass.equals(StopCommunication.class)) {
            stop(session);
        }
    }

    private synchronized void presenter(WebSocketSession session, PresenterOfferMessage message) {
        if (presenterUserSession == null) {
            presenterUserSession = new UserSession(session);

            pipeline = kurento.createMediaPipeline();
            presenterUserSession.setWebRtcEndpoint(new WebRtcEndpoint.Builder(pipeline).build());

            var presenterWebRtc = presenterUserSession.getWebRtcEndpoint();

            presenterWebRtc.addIceCandidateFoundListener(event -> {
                IceCandidateResponse iceCandidate = new IceCandidateResponse(event.getCandidate());
                synchronized (session) {
                    sendMessage(session, iceCandidate);
                }
            });

            var sdpOffer = message.getSdpOffer();
            var sdpAnswer = presenterWebRtc.processOffer(sdpOffer);
            var response = new AcceptedPresenterResponse(sdpAnswer);
            synchronized (session) {
                sendMessage(presenterUserSession, response);
            }
            presenterWebRtc.gatherCandidates();
            return;
        }
        var response = new RejectedPresenterResponse("Another user is currently acting as sender. Try again later ...");
        sendMessage(session, response);
    }

    private void viewer(WebSocketSession session, ViewerOfferMessage message) {
        if (presenterUserSession == null) {
            var response = new RejectedViewerResponse("No active sender now. Become sender or . Try again later ...");
            sendMessage(session, response);
            return;
        }
        if (viewers.containsKey(session.getId())) {
            var response = new RejectedViewerResponse("You are already viewing in this session. "
                    + "Use a different browser to add additional viewers.");
            sendMessage(session, response);
            return;
        }
        UserSession viewer = new UserSession(session);
        viewers.put(session.getId(), viewer);

        WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(pipeline).build();

        nextWebRtc.addIceCandidateFoundListener(event -> {
            IceCandidateResponse iceCandidate = new IceCandidateResponse(event.getCandidate());
            synchronized (session) {
                sendMessage(session, iceCandidate);
            }
        });

        viewer.setWebRtcEndpoint(nextWebRtc);
        presenterUserSession.getWebRtcEndpoint().connect(nextWebRtc);

        String sdpOffer = message.getSdpOffer();
        String sdpAnswer = nextWebRtc.processOffer(sdpOffer);
        var response = new AcceptedViewerResponse(sdpAnswer);
        synchronized (session) {
            sendMessage(viewer, response);
        }
        nextWebRtc.gatherCandidates();
    }

    private synchronized void stop(WebSocketSession session) {
        String sessionId = session.getId();
        if (presenterUserSession != null && presenterUserSession.getSession().getId().equals(sessionId)) {
            for (UserSession viewer : viewers.values()) {
                var stopCommunication = new StopCommunication();
                sendMessage(viewer, stopCommunication);
            }

            log.info("Releasing media pipeline");
            if (pipeline != null) {
                pipeline.release();
            }
            pipeline = null;
            presenterUserSession = null;
            return;
        }
        if (viewers.containsKey(sessionId)) {
            if (viewers.get(sessionId).getWebRtcEndpoint() != null) {
                viewers.get(sessionId).getWebRtcEndpoint().release();
            }
            viewers.remove(sessionId);
        }
    }

    private void handleIceCandidate(WebSocketSession session, IceCandidateOffer msg) {
        UserSession user = null;
        // Think about
        if (presenterUserSession != null) {
            if (presenterUserSession.getSession() == session) {
                user = presenterUserSession;
            } else {
                user = viewers.get(session.getId());
            }
        }
        if (user != null) {
            user.addCandidate(msg.getCandidate());
        }
    }

    private void sendMessage(WebSocketSession session, Message message) {
        var json = converterService.toJsonString(message);
        log.info("Send message: {}", json);
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(UserSession session, Message message) {
        sendMessage(session.getSession(), message);
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
        stop(session);
    }
}
