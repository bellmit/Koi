package co.casterlabs.koi.events;

import java.util.Collection;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@NonNull
@ToString
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
