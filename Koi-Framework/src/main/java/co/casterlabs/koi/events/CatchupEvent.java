package co.casterlabs.koi.events;

import java.util.List;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@NonNull
@EqualsAndHashCode(callSuper = false)
public class CatchupEvent extends Event {
    private List<Event> events;
    private User streamer;

    public CatchupEvent(User streamer, List<Event> events) {
        this.streamer = streamer;
        this.events = events;
    }

    @Override
    public EventType getType() {
        return EventType.CATCHUP;
    }

}
