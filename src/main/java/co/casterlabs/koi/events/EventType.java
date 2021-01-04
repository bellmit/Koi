package co.casterlabs.koi.events;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;

@Getter
public enum EventType {
    FOLLOW,
    CHAT,
    DONATION,
    SUBSCRIPTION,
    USER_UPDATE,
    STREAM_STATUS,
    UPVOTE,
    VIEWER_JOIN,
    VIEWER_LEAVE,
    VIEWER_LIST;

    private static final String[] messages = new String[] {
            "I like pancakes",
            "DON'T CLICK THAT!",
            "Have some candy!",
            "Check out our Twitter!",
            "Check out our Discord!"
    };

    public Event getTestEvent() {
        // Is it bad? YES. Do I care? NO.
        switch (this) {
            case CHAT:
                return new ChatEvent("", randomMessage(), getCasterlabsUser(), getCasterlabsUser());

            case DONATION:
                return new DonationEvent("", randomMessage(), getCasterlabsUser(), getCasterlabsUser(), Arrays.asList(new Donation("https://static-cdn.jtvnw.net/bits/dark/static/gray/4", "TWITCH BITS", 0, "https://static-cdn.jtvnw.net/bits/dark/animated/gray/4")));

            case FOLLOW:
                return new FollowEvent(getCasterlabsUser(), getCasterlabsUser());

            case SUBSCRIPTION:
                return new SubscriptionEvent(getCasterlabsUser(), getCasterlabsUser(), 0);

            default:
                return null;

        }
    }

    private static User getCasterlabsUser() {
        return UserPlatform.TWITCH.getConverter().get("Casterlabs");
    }

    private static String randomMessage() {
        return messages[ThreadLocalRandom.current().nextInt(messages.length)];
    }

}
