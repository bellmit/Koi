package co.casterlabs.koi.events;

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
public class RaidEvent extends Event {
    private User host;
    private User streamer;
    private int viewers;

    @Override
    public EventType getType() {
        return EventType.RAID;
    }

}
