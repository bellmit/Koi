package co.casterlabs.koi.events;

import lombok.Getter;

@Getter
public enum EventType {
    INFO,
    FOLLOW("follower"),
    CHAT,
    DONATION("sender"),
    SUBSCRIPTION,
    USER_UPDATE,
    STREAM_STATUS,
    UPVOTE;

    // For events that may get stored, so Koi and clients can only store the UUID and platform and then retrive up-to-date user information.
    private String otherUser = null;
    private boolean data = false;

    private EventType() {}

    private EventType(String otherUser) {
        this.data = true;
        this.otherUser = otherUser;
    }

}
