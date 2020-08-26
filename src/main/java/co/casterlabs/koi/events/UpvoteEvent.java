package co.casterlabs.koi.events;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

@Data
@EqualsAndHashCode(callSuper = false)
public class UpvoteEvent extends Event {
    private ChatEvent event;
    private User streamer;
    private int upvotes;
    private String id;

    @SneakyThrows
    public UpvoteEvent(ChatEvent event, int upvotes) {
        this.streamer = event.getStreamer();
        this.id = event.getId();
        this.upvotes = upvotes;
        this.event = event;
    }

    @Override
    public EventType getType() {
        return EventType.UPVOTE;
    }

}
