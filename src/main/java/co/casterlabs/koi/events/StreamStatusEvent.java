package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.SerializedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class StreamStatusEvent extends Event {
    @SerializedName("is_live")
    private boolean live;
    private String title;
    private SerializedUser streamer;

    @Override
    public EventType getType() {
        return EventType.STREAM_STATUS;
    }

}
