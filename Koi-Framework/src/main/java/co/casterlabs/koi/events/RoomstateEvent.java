package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@NonNull
@ToString
@Accessors(chain = true)
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RoomstateEvent extends Event {
    private final User streamer;

    @Setter
    @SerializedName("is_emote_only")
    private boolean isEmoteOnly = false;

    @Setter
    @SerializedName("is_subs_only")
    private boolean isSubsOnly = false;

    @Setter
    @SerializedName("is_r9k")
    private boolean isR9K = false;

    @Setter
    @SerializedName("is_followers_only")
    private boolean isFollowersOnly = false;

    @Setter
    @SerializedName("is_slowmode")
    private boolean isSlowMode = false;

    @Override
    public EventType getType() {
        return EventType.ROOMSTATE;
    }

}
