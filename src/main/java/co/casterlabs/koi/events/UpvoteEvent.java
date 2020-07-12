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
public class UpvoteEvent extends Event {
    private JsonObject event;
    private User streamer;
    private int upvotes;

    @SneakyThrows
    public UpvoteEvent(Event event, int upvotes) {
        this.streamer = event.getStreamer();
        this.event = event.serialize();
        this.upvotes = upvotes;
    }

    @Override
    public EventType getType() {
        return EventType.UPVOTE;
    }

}
