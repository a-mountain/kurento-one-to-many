package com.example.server.messages.in;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartWatchMessage implements Message {
    String sdpOffer;
}
