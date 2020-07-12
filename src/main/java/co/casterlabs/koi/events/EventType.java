package co.casterlabs.koi.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {
    INFO,
    FOLLOW(true, "follower"),
    CHAT,
    SHARE,
    DONATION(true, "sender"),
    SUBSCRIPTION,
    USER_UPDATE,
    STREAM_STATUS,
    UPVOTE;

    private boolean data;
    private String otherUser;

    private EventType() {
        this(false, null);
    }

}
