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
public class SubscriptionEvent extends Event {
    private User sender;
    private long time;

    @SneakyThrows
    public SubscriptionEvent(User sender, User streamer, long time) {
        this.sender = sender;
        this.streamer = streamer;
        this.time = time;
    }

    @Override
    public EventType getType() {
        return EventType.SUBSCRIPTION;
    }

    @Override
    protected void serialize0(JsonObject json) {
        json.addProperty("time", this.time);
        json.add("sender", this.sender.serialize());
    }

}
