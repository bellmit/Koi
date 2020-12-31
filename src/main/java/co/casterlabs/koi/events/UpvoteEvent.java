package co.casterlabs.koi.events;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

@Data
@EqualsAndHashCode(callSuper = false)
public class UpvoteEvent extends Event {
    private User streamer;
    private int upvotes;
    private String id;

    @SneakyThrows
    public UpvoteEvent(User streamer, String id, int upvotes) {
        this.streamer = streamer;
        this.id = id;
        this.upvotes = upvotes;
    }

    @Override
    public EventType getType() {
        return EventType.UPVOTE;
    }

}
