package com.example.server.services;

import com.example.server.domain.UserSession;
import com.example.server.messages.bi.IceCandidateMessage;
import com.example.server.messages.bi.StopCommunicationMessage;
import com.example.server.messages.in.StartStreamMessage;
import com.example.server.messages.in.StartWatchMessage;
import com.example.server.messages.out.RejectedPresenterMessage;
import com.example.server.messages.out.RejectedViewerMessage;
import com.example.server.messages.out.SdpAnswerMessage;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CallService {

    private final MessagingService messagingService;
    private final KurentoClient kurento;
    private final Map<String, UserSession> viewers = new ConcurrentHashMap<>();

    private MediaPipeline pipeline;
    private UserSession presenterUserSession;

    @Autowired
    public CallService(MessagingService messagingService, KurentoClient kurento) {
        this.messagingService = messagingService;
        this.kurento = kurento;
    }

    public synchronized void startStreaming(String sessionId, StartStreamMessage message) {
        if (presenterUserSession == null) {
            pipeline = kurento.createMediaPipeline();
            var presenterWebRtc = new WebRtcEndpoint.Builder(pipeline).build();
            presenterUserSession = newUserSession(sessionId, presenterWebRtc);

            presenterWebRtc.addIceCandidateFoundListener(event -> iceCandidateFoundListener(event, sessionId));

            var sdpOffer = message.getSdpOffer();
            var sdpAnswer = presenterWebRtc.processOffer(sdpOffer);
            var response = new SdpAnswerMessage(sdpAnswer);
            presenterUserSession.sendMessage(response);
            presenterWebRtc.gatherCandidates();
            return;
        }
        var response = new RejectedPresenterMessage("Another user is currently acting as sender. Try again later ...");
        messagingService.sendMessage(sessionId, response);
    }

    public synchronized void startWatch(String sessionId, StartWatchMessage message) {
        if (presenterUserSession == null) {
            var response = new RejectedViewerMessage("No active sender now. Become sender or . Try again later ...");
            messagingService.sendMessage(sessionId, response);
            return;
        }
        if (viewers.containsKey(sessionId)) {
            var response = new RejectedViewerMessage("You are already viewing in this session. "
                    + "Use a different browser to add additional viewers.");
            messagingService.sendMessage(sessionId, response);
            return;
        }

        var nextWebRtc = new WebRtcEndpoint.Builder(pipeline).build();
        var viewer = newUserSession(sessionId, nextWebRtc);
        viewers.put(sessionId, viewer);
        nextWebRtc.addIceCandidateFoundListener(event -> iceCandidateFoundListener(event, sessionId));
        presenterUserSession.getWebRtcEndpoint().connect(nextWebRtc);

        String sdpOffer = message.getSdpOffer();
        String sdpAnswer = nextWebRtc.processOffer(sdpOffer);
        var response = new SdpAnswerMessage(sdpAnswer);

        viewer.sendMessage(response);
        nextWebRtc.gatherCandidates();
    }

    public synchronized void stop(String sessionId) {
        if (presenterUserSession != null && isStreamerSession(sessionId)) {
            stopStreamer();
        }
        if (viewers.containsKey(sessionId)) {
            stopViewer(sessionId);
        }
    }

    public void handleIceCandidate(String sessionId, com.example.server.messages.bi.IceCandidateMessage msg) {
        UserSession user = null;
        // Think about
        if (presenterUserSession != null) {
            if (presenterUserSession.getSessionId().equals(sessionId)) {
                user = presenterUserSession;
            } else {
                user = viewers.get(sessionId);
            }
        }
        if (user != null) {
            user.addCandidate(msg.getCandidate());
        }
    }

    private void stopStreamer() {
        for (UserSession viewer : viewers.values()) {
            var stopCommunication = new StopCommunicationMessage();
            viewer.sendMessage(stopCommunication);
        }

        log.info("Releasing media pipeline");
        if (pipeline != null) {
            pipeline.release();
        }
        pipeline = null;
        presenterUserSession = null;
    }

    private void stopViewer(String sessionId) {
        if (viewers.get(sessionId).getWebRtcEndpoint() != null) {
            viewers.get(sessionId).getWebRtcEndpoint().release();
        }
        viewers.remove(sessionId);
    }

    private void iceCandidateFoundListener(IceCandidateFoundEvent event, String sessionId) {
        var iceCandidate = new IceCandidateMessage(event.getCandidate());
        messagingService.sendMessage(sessionId, iceCandidate);
    }

    private boolean isStreamerSession(String sessionId) {
        return presenterUserSession.getSessionId().equals(sessionId);
    }

    private UserSession newUserSession(String sessionId, WebRtcEndpoint webRtcEndpoint) {
        return new UserSession(sessionId, webRtcEndpoint, messagingService);
    }
}
