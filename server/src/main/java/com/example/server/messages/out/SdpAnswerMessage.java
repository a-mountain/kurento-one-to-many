package com.example.server.messages.out;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SdpAnswerMessage implements Message {
    private String sdpAnswer;
}
