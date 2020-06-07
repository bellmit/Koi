package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class FollowEvent extends Event {
    private User follower;

    @SneakyThrows
    public FollowEvent(User follower, User streamer) {
        this.streamer = streamer;
        this.follower = follower;
    }

    @Override
    public EventType getType() {
        return EventType.FOLLOW;
    }

    @Override
    protected void serialize0(JsonObject json) {
        json.add("follower", this.follower.serialize());
    }

}
