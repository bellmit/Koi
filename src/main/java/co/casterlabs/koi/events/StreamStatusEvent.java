package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class StreamStatusEvent extends Event {
    @SerializedName("is_live")
    private boolean live;
    private String title;
    private User streamer;

    @Override
    public EventType getType() {
        return EventType.STREAM_STATUS;
    }

}
