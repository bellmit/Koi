package co.casterlabs.koi.events;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class UserUpdateEvent extends Event {
    private User streamer;

    @Override
    public EventType getType() {
        return EventType.USER_UPDATE;
    }

}
