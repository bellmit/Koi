package co.casterlabs.koi.events;

import co.casterlabs.koi.user.SerializedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ViewerLeaveEvent extends Event {
    private SerializedUser viewer;
    private SerializedUser streamer;

    @Override
    public EventType getType() {
        return EventType.VIEWER_LEAVE;
    }

}
