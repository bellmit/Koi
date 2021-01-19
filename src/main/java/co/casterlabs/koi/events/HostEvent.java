package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HostEvent extends Event {
    private User host;
    private User streamer;
    private long viewers;
    @SerializedName("host_type")
    private HostEventType hostType;

    @Override
    public EventType getType() {
        return EventType.HOST;
    }

    public static enum HostEventType {
        START,
        STOP

    }

}
