package co.casterlabs.koi.events;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.DonationEvent.DonationType;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionLevel;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionType;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;

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
    VIEWER_LIST,
    RAID,
    CHANNEL_POINTS;

    private static final @Getter User systemUser = new User(UserPlatform.CASTERLABS_SYSTEM);
    private static final User casterlabsUser = new User(UserPlatform.CASTERLABS_SYSTEM);

    private static final String[] messages = new String[] {
            "I like pancakes",
            "DON'T CLICK THAT!",
            "Have some candy!",
            "Check out our Twitter!",
            "Check out our Discord!"
    };

    static {
        casterlabsUser.setUsername("casterlabs");
        casterlabsUser.setDisplayname("Casterlabs");
        casterlabsUser.setUUID("CASTERLABS_SYSTEM");
        casterlabsUser.setImageLink("https://assets.casterlabs.co/logo/casterlabs_icon.png");
        casterlabsUser.setColor("#ea4c4c");

        systemUser.setUsername("casterlabs");
        systemUser.setDisplayname("Casterlabs");
        systemUser.setUUID("CASTERLABS_SYSTEM");
        systemUser.setImageLink("https://assets.casterlabs.co/logo/casterlabs_icon.png");
        systemUser.setColor("#ea4c4c");
    }

    public Event getTestEvent() {
        // Is it bad? YES. Do I care? NO.
        switch (this) {
            case CHAT:
                return new ChatEvent("", randomMessage(), casterlabsUser, casterlabsUser);

            case DONATION:
                return new DonationEvent("", randomMessage(), casterlabsUser, casterlabsUser, Arrays.asList(new Donation("https://static-cdn.jtvnw.net/bits/dark/static/gray/4", "TWITCH_BITS", 0, "https://static-cdn.jtvnw.net/bits/dark/animated/gray/4", DonationType.CASTERLABS_TEST)));

            case FOLLOW:
                return new FollowEvent(casterlabsUser, casterlabsUser);

            case SUBSCRIPTION:
                return new SubscriptionEvent(casterlabsUser, casterlabsUser, 0, null, SubscriptionType.SUB, SubscriptionLevel.TIER_1);

            case RAID:
                return new RaidEvent(casterlabsUser, casterlabsUser, 0);

            default:
                return null;

        }
    }

    private static String randomMessage() {
        return messages[ThreadLocalRandom.current().nextInt(messages.length)];
    }

}
