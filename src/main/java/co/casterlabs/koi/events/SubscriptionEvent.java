package co.casterlabs.koi.events;

import co.casterlabs.koi.user.SerializedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubscriptionEvent extends Event {
    private SerializedUser subscriber;
    private SerializedUser streamer;
    private int months;

    @Override
    public EventType getType() {
        return EventType.SUBSCRIPTION;
    }

}
