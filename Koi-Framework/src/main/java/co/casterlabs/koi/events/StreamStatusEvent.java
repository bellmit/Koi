package co.casterlabs.koi.events;

import java.time.Instant;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@NonNull
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class StreamStatusEvent extends Event {
    @SerializedName("is_live")
    private boolean live;
    private String title;
    private User streamer;
    @SerializedName("start_time")
    private @Setter Instant startTime;

    @Override
    public EventType getType() {
        return EventType.STREAM_STATUS;
    }

}
