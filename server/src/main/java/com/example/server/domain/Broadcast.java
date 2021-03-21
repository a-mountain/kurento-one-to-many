package com.example.server.domain;

import com.example.server.media.User;

import java.util.List;

public class Broadcast {

    private final User owner;
    private boolean isOnline;
    private final List<User> viewers;

    public Broadcast(User owner, boolean isOnline, List<User> viewers) {
        this.owner = owner;
        this.isOnline = isOnline;
        this.viewers = viewers;
    }

    private void watch(User user) {

    }

    private void start() {

    }
}
