package co.casterlabs.koi.events;

import java.util.Collection;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ViewerListEvent extends Event {
    private Collection<User> viewers;
    private User streamer;

    @Override
    public EventType getType() {
        return EventType.VIEWER_LIST;
    }

}
