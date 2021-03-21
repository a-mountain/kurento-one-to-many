package com.example.server.services;

import com.example.server.media.UserSession;
import com.example.server.messages.bi.IceCandidateMessage;
import com.example.server.messages.bi.StopCommunicationMessage;
import com.example.server.messages.in.StartStreamMessage;
import com.example.server.messages.in.StartWatchMessage;
import com.example.server.messages.out.RejectionMessage;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class KurentoService {

    private final MessagingService messagingService;
    private final KurentoClient kurento;
    private final Map<String, UserSession> viewers = new ConcurrentHashMap<>();

    private MediaPipeline pipeline;
    private UserSession streamerSession;

    @Autowired
    public KurentoService(MessagingService messagingService, KurentoClient kurento) {
        this.messagingService = messagingService;
        this.kurento = kurento;
    }

    public synchronized void startStreaming(String sessionId, StartStreamMessage message) {
        if (streamerSession == null) {
            createStreamer(sessionId, message);
        } else {
            sendStreamerAlreadyExistsMessage(sessionId);
        }
    }

    public synchronized void startWatch(String sessionId, StartWatchMessage message) {
        if (streamerSession == null) {
            sendNoSenderMessage(sessionId);
            return;
        }
        if (viewers.containsKey(sessionId)) {
            sendAlreadyViewerMessage(sessionId);
            return;
        }
        createViewer(sessionId, message);
    }

    public synchronized void stop(String sessionId) {
        if (streamerSession != null && isStreamerSession(sessionId)) {
            stopStreamer();
        }
        if (viewers.containsKey(sessionId)) {
            stopViewer(sessionId);
        }
    }

    public void handleIceCandidate(String sessionId, IceCandidateMessage msg) {
        if (streamerSession != null && isStreamerSession(sessionId)) {
            streamerSession.addCandidate(msg.getCandidate());
            return;
        }
        if (viewers.containsKey(sessionId)) {
            var viewerSession = viewers.get(sessionId);
            viewerSession.addCandidate(msg.getCandidate());
        }
    }

    private void sendStreamerAlreadyExistsMessage(String sessionId) {
        var response = new RejectionMessage("Another user is currently acting as sender. Try again later ...");
        messagingService.sendMessage(sessionId, response);
    }

    private void createStreamer(String sessionId, StartStreamMessage message) {
        pipeline = kurento.createMediaPipeline();
        streamerSession = newUserSession(sessionId);
        var sdpOffer = message.getSdpOffer();
        streamerSession.processOffer(sdpOffer);
    }

    private void sendNoSenderMessage(String sessionId) {
        var response = new RejectionMessage("No active sender now. Become sender or . Try again later ...");
        messagingService.sendMessage(sessionId, response);
    }

    private void sendAlreadyViewerMessage(String sessionId) {
        var response = new RejectionMessage("You are already viewing in this session. "
                + "Use a different browser to add additional viewers.");
        messagingService.sendMessage(sessionId, response);
    }

    private void createViewer(String sessionId, StartWatchMessage message) {
        var viewer = newUserSession(sessionId);
        streamerSession.connect(viewer);
        viewers.put(sessionId, viewer);
        var sdpOffer = message.getSdpOffer();
        viewer.processOffer(sdpOffer);
    }

    private void stopStreamer() {
        stopAllViewers();
        releaseMediaPipeline();
        streamerSession = null;
    }

    private void releaseMediaPipeline() {
        log.info("Releasing media pipeline");
        if (pipeline != null) {
            pipeline.release();
        }
        pipeline = null;
    }

    private void stopViewer(String sessionId) {
        if (viewers.containsKey(sessionId)) {
            var userSession = viewers.get(sessionId);
            userSession.release();
            viewers.remove(sessionId);
        }
    }

    private void stopAllViewers() {
        for (var viewer : viewers.values()) {
            var stopCommunication = new StopCommunicationMessage();
            viewer.sendMessage(stopCommunication);
        }
    }

    private boolean isStreamerSession(String sessionId) {
        return streamerSession.getSessionId().equals(sessionId);
    }

    private UserSession newUserSession(String sessionId) {
        return new UserSession(sessionId, messagingService, pipeline);
    }
}
