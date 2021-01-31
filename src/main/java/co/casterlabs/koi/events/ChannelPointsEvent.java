package co.casterlabs.koi.events;

import java.time.Instant;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ChannelPointsEvent extends Event {
    private User sender;
    private User streamer;
    private ChannelPointsReward reward;
    private RedemptionStatus status;
    private String id;

    @Override
    public EventType getType() {
        return EventType.CHANNEL_POINTS;
    }

    public static enum RedemptionStatus {
        FULFILLED,
        UNFULFILLED
    }

    @Getter
    @Setter
    @ToString
    public static class ChannelPointsReward {

        @SerializedName("background_color")
        private String backgroundColor;

        private String id;

        @SerializedName("cooldown_expires_at")
        private Instant cooldownExpiresAt;

        private String title;

        private String prompt;

        private int cost;

        @SerializedName("is_enabled")
        private boolean enabled;

        @SerializedName("is_in_stock")
        private boolean inStock;

        @SerializedName("is_paused")
        private boolean paused;

        @SerializedName("is_sub_only")
        private boolean subOnly;

        @SerializedName("is_user_input_required")
        private boolean userInputRequired;

        @SerializedName("reward_image")
        private String rewardImage;

        @SerializedName("default_reward_image")
        private String defaultRewardImage;

        @SerializedName("max_per_stream")
        private ChannelPointsMaxPerStream maxPerStream;

        @SerializedName("max_per_user_per_stream")
        private ChannelPointsMaxPerUserPerStream maxPerUserPerStream;

        @SerializedName("global_cooldown")
        private ChannelPointsCooldown globalCooldown;

    }

    @Getter
    @ToString
    public static class ChannelPointsMaxPerStream {

        @SerializedName("is_enabled")
        private boolean enabled;

        @SerializedName("max_per_stream")
        private int max;

    }

    @Getter
    @ToString
    public static class ChannelPointsMaxPerUserPerStream {

        @SerializedName("is_enabled")
        private boolean enabled;

        @SerializedName("max_per_user_per_stream")
        private int max;

    }

    @Getter
    @ToString
    public static class ChannelPointsCooldown {

        @SerializedName("is_enabled")
        private boolean enabled;

        @SerializedName("global_cooldown_seconds")
        private int globalCooldownSeconds;

    }

}
