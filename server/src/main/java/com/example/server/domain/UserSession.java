package com.example.server.domain;

import com.example.server.messages.Message;
import com.example.server.messages.bi.IceCandidateMessage;
import com.example.server.messages.out.SdpAnswerMessage;
import com.example.server.services.MessagingService;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

public class UserSession {

    private final String sessionId;
    private final MessagingService messagingService;
    private final WebRtcEndpoint endpoint;

    public UserSession(String sessionId, MessagingService messagingService, MediaPipeline pipeline) {
        this.sessionId = sessionId;
        this.messagingService = messagingService;
        endpoint = new WebRtcEndpoint.Builder(pipeline).build();
        endpoint.addIceCandidateFoundListener(this::iceCandidateFoundListener);
    }

    public void addCandidate(IceCandidate candidate) {
        endpoint.addIceCandidate(candidate);
    }

    public void sendMessage(Message message) {
        messagingService.sendMessage(sessionId, message);
    }

    public void release() {
        endpoint.release();
    }

    public void connect(UserSession session) {
        this.endpoint.connect(session.endpoint);
    }

    public void processOffer(String sdpOffer) {
        var sdpAnswer = endpoint.processOffer(sdpOffer);
        var response = new SdpAnswerMessage(sdpAnswer);
        sendMessage(response);
        endpoint.gatherCandidates();
    }

    private void iceCandidateFoundListener(IceCandidateFoundEvent event) {
        var iceCandidate = new IceCandidateMessage(event.getCandidate());
        sendMessage(iceCandidate);
    }

    public String getSessionId() {
        return sessionId;
    }
}
