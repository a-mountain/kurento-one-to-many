package com.example.server.messages.bi;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kurento.client.IceCandidate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IceCandidateMessage implements Message {
    private IceCandidate candidate;
}
