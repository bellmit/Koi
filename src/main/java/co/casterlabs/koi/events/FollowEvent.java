package co.casterlabs.koi.events;

import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FollowEvent extends Event {
    private SerializedUser follower;
    private User streamer;

    @Override
    public EventType getType() {
        return EventType.FOLLOW;
    }

}
