package com.example.server.messages.out;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedViewerResponse implements Message {
    private String rejectionReasonMessage;
}
