package co.casterlabs.koi.events;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class ShareEvent extends Event {
    private String message;
    private User sender;
    private User streamer;

    @Override
    public EventType getType() {
        return EventType.SHARE;
    }

}
