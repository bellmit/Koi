package co.casterlabs.koi.events;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RaidEvent extends Event {
    private User host;
    private User streamer;
    private int viewers;

    @Override
    public EventType getType() {
        return EventType.RAID;
    }

}
