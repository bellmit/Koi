package co.casterlabs.koi.events;

import java.util.List;

import com.google.gson.JsonArray;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@NonNull
@EqualsAndHashCode(callSuper = false)
public class CatchupEvent extends Event {
    private JsonArray events = new JsonArray();
    private User streamer;

    public CatchupEvent(User streamer, List<ChatEvent> events) {
        this.streamer = streamer;

        for (ChatEvent event : events) {
            this.events.add(event.serialize());
        }
    }

    @Override
    public EventType getType() {
        return EventType.CATCHUP;
    }

}
