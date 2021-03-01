package com.example.server.messages.in;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kurento.client.IceCandidate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IceCandidateOffer implements Message {
    private IceCandidate candidate;
}
