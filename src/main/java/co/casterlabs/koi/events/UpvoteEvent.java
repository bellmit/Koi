package co.casterlabs.koi.events;

import co.casterlabs.koi.user.SerializedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

@Data
@EqualsAndHashCode(callSuper = false)
public class UpvoteEvent extends Event {
    private SerializedUser streamer;
    private int upvotes;
    private String id;

    @SneakyThrows
    public UpvoteEvent(SerializedUser streamer, String id, int upvotes) {
        this.streamer = streamer;
        this.id = id;
        this.upvotes = upvotes;
    }

    @Override
    public EventType getType() {
        return EventType.UPVOTE;
    }

}
