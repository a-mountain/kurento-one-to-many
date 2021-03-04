package com.example.server.lib;

import com.example.server.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TypedHandler<T extends Message> {
    private final Class<T> messageType;
    private final MessageHandler<T> handler;
}
