package co.casterlabs.koi.events;

import lombok.Getter;

@Getter
public enum EventType {
    INFO,
    FOLLOW,
    CHAT,
    DONATION,
    SUBSCRIPTION,
    USER_UPDATE,
    STREAM_STATUS,
    UPVOTE;

}
