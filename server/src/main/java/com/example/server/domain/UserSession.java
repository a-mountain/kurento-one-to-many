package com.example.server.domain;

import com.example.server.messages.Message;
import com.example.server.services.MessagingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;

@Data
@AllArgsConstructor
public class UserSession {

    private final String sessionId;
    private final WebRtcEndpoint webRtcEndpoint;
    private final MessagingService messagingService;

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
    }

    public void sendMessage(Message message) {
        messagingService.sendMessage(sessionId, message);
    }
}
