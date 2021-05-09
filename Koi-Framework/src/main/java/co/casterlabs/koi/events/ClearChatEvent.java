package co.casterlabs.koi.events;

import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class ClearChatEvent extends Event {
    private @NonNull User streamer;

    @SerializedName("user_upid")
    private @Nullable String userUPID;

    @SerializedName("clear_type")
    private @NonNull ClearChatType clearType;

    public ClearChatEvent(@NonNull User streamer, @Nullable String userUPID) {
        this.streamer = streamer;
        this.userUPID = userUPID;

        this.clearType = (this.userUPID == null) ? ClearChatType.ALL : ClearChatType.USER;
    }

    @Override
    public EventType getType() {
        return EventType.CLEARCHAT;
    }

    public static enum ClearChatType {
        ALL,
        USER;

    }

}
