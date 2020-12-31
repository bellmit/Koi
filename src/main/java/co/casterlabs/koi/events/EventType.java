package co.casterlabs.koi.events;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
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
    UPVOTE,
    VIEWER_JOIN, // TODO
    VIEWER_LEAVE, // TODO
    VIEWER_LIST; // TODO

    private static final String[] messages = new String[] {
            "I like pancakes",
            "DON'T CLICK THAT!",
            "Have some candy!",
            "Check out our Twitter!",
            "Check out our Discord!"
    };

    private static User casterlabs = new User(UserPlatform.TWITCH);

    public Event getTestEvent() {
        // Is it bad? YES. Do I care? NO.
        switch (this) {
            case CHAT:
                return new ChatEvent("", randomMessage(), casterlabs, casterlabs);

            case DONATION:
                return new DonationEvent("", randomMessage(), casterlabs, casterlabs, Arrays.asList(new Donation("https://assets.caffeine.tv/digital-items/wave.58c9cc9c26096f3eb6f74f13603b5515.png", "USD", 0, "https://static-cdn.jtvnw.net/bits/dark/animated/purple/4")));

            case FOLLOW:
                return new FollowEvent(casterlabs, casterlabs);

            case SUBSCRIPTION:
                return new SubscriptionEvent(casterlabs, casterlabs, 0);

            default:
                return null;

        }
    }

    private static String randomMessage() {
        return messages[ThreadLocalRandom.current().nextInt(messages.length)];
    }

}
