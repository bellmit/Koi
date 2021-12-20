package co.casterlabs.koi.events;

import java.util.UUID;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@NonNull
@ToString
@EqualsAndHashCode(callSuper = false)
public class PlatformMessageEvent extends ChatEvent {
    @SerializedName("is_error")
    private boolean isError;

    public PlatformMessageEvent(@NonNull String message, @NonNull UserPlatform platform, @NonNull User streamer, boolean isError) {
        super(UUID.randomUUID().toString(), message, platform.getPlatformUser(), streamer);

        this.isError = isError;
    }

    @Override
    public EventType getType() {
        return EventType.PLATFORM_MESSAGE;
    }

}
