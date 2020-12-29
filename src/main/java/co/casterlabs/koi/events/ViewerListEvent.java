package co.casterlabs.koi.events;

import java.util.List;

import co.casterlabs.koi.user.SerializedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ViewerListEvent extends Event {
    private List<SerializedUser> viewers;
    private SerializedUser streamer;

    @Override
    public EventType getType() {
        return EventType.VIEWER_LIST;
    }

}
