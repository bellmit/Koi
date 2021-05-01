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
    META,
    VIEWER_JOIN,
    VIEWER_LEAVE,
    VIEWER_LIST,
    RAID,
    CHANNEL_POINTS;

    private static final @Getter User anonymousUser = new User(UserPlatform.CASTERLABS_SYSTEM);
    private static final @Getter User systemUser = new User(UserPlatform.CASTERLABS_SYSTEM);

    private static final User casterlabsUser = new User(UserPlatform.CASTERLABS_SYSTEM);

    private static final String[] messages = new String[] {
            "I like pancakes",
            "DON'T CLICK THAT!",
            "Have some candy!",
            "Check out our Twitter!",
            "Check out our Discord!"
    };

    private static final SubscriptionEvent[] randomSubEvents = new SubscriptionEvent[SubscriptionType.values().length];

    static {
        casterlabsUser.setUsername("casterlabs");
        casterlabsUser.setDisplayname("Casterlabs");
        casterlabsUser.setUUID("CASTERLABS_SYSTEM");
        casterlabsUser.setImageLink("https://assets.casterlabs.co/logo/casterlabs_icon.png");
        casterlabsUser.setColor("#ea4c4c");

        systemUser.setUsername("casterlabs");
        systemUser.setDisplayname("Casterlabs-System");
        systemUser.setUUID("CASTERLABS_SYSTEM");
        systemUser.setImageLink("https://assets.casterlabs.co/logo/casterlabs_icon.png");
        systemUser.setColor("#ea4c4c");

        anonymousUser.setUsername("anonymous");
        anonymousUser.setDisplayname("Anonymous");
        anonymousUser.setUUID("CASTERLABS_SYSTEM_ANONYMOUS");
        anonymousUser.setImageLink("data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==");
        anonymousUser.setColor("#ea4c4c");

        for (int i = 0; i < randomSubEvents.length; i++) {
            SubscriptionType type = SubscriptionType.values()[i];

            randomSubEvents[i] = new SubscriptionEvent(casterlabsUser, casterlabsUser, 0, casterlabsUser, type, SubscriptionLevel.TIER_1);
        }
    }

    public Event getTestEvent() {
        // Is it bad? YES. Do I care? NO.
        switch (this) {
            case CHAT:
                return new ChatEvent("", randomMessage(), casterlabsUser, casterlabsUser);

            case DONATION:
                return new DonationEvent("", randomMessage(), casterlabsUser, casterlabsUser, Arrays.asList(new Donation("https://static-cdn.jtvnw.net/bits/dark/static/gray/4", "TWITCH_BITS", 0, "https://static-cdn.jtvnw.net/bits/dark/animated/gray/4", DonationType.CASTERLABS_TEST, "Test Donation")));

            case FOLLOW:
                return new FollowEvent(casterlabsUser, casterlabsUser);

            case SUBSCRIPTION:
                return randomSubEvents[ThreadLocalRandom.current().nextInt(randomSubEvents.length)];

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
