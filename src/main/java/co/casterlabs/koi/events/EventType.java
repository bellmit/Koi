package co.casterlabs.koi.events;

import lombok.Getter;

public enum EventType {
    INFO,
    FOLLOW(true),
    CHAT,
    SHARE,
    DONATE(true),
    SUBSCRIPTION,
    STREAM_STATUS;

    private @Getter boolean data;

    private EventType() {
        this(false);
    }

    private EventType(boolean data) {
        this.data = data;
    }

}
