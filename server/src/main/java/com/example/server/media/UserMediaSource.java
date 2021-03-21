package com.example.server.media;

public interface UserMediaSource {

    void sendMedia(UserMediaSource userMediaSource);
    boolean isStreaming();
    boolean isReceiving();
}
