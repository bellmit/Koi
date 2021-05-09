package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@NonNull
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubscriptionEvent extends Event {
    private User subscriber;
    private User streamer;
    @Deprecated
    private int months;

    @SerializedName("gift_recipient")
    private User giftRecipient;

    @SerializedName("sub_type")
    private SubscriptionType subType;

    @SerializedName("sub_level")
    private SubscriptionLevel subLevel;

    @Override
    public EventType getType() {
        return EventType.SUBSCRIPTION;
    }

    public static enum SubscriptionType {
        SUB,
        RESUB,

        SUBGIFT,
        RESUBGIFT,

        ANONSUBGIFT,
        ANONRESUBGIFT;

    }

    public static enum SubscriptionLevel {
        UNKNOWN,
        TWITCH_PRIME,
        TIER_1,
        TIER_2,
        TIER_3,
        TIER_4,
        TIER_5;

    }

}
