package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class SubscriptionEvent extends Event {
    private JsonObject subscriber;
    private User streamer;
    private long time;

    @Override
    public EventType getType() {
        return EventType.SUBSCRIPTION;
    }

}
