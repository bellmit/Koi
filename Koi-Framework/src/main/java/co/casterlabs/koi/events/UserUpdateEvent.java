package co.casterlabs.koi.events;

import java.time.Instant;

import co.casterlabs.koi.user.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@NonNull
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserUpdateEvent extends Event {
    private String timestamp = Instant.now().toString();
    private @NonNull User streamer;

    @Override
    public EventType getType() {
        return EventType.USER_UPDATE;
    }

}
