package com.example.server.messages.out;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectionMessage implements Message {
    private String reason;
}
