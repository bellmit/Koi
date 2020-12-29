package co.casterlabs.koi.events;

import java.time.Instant;

import co.casterlabs.koi.user.SerializedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserUpdateEvent extends Event {
    private String timestamp = Instant.now().toString();
    private @NonNull SerializedUser streamer;

    @Override
    public EventType getType() {
        return EventType.USER_UPDATE;
    }

}
