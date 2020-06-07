package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class StreamStatusEvent extends Event {
    private boolean live;

    @SneakyThrows
    public StreamStatusEvent(boolean isLive, User streamer) {
        this.streamer = streamer;
        this.live = isLive;
    }

    @Override
    public EventType getType() {
        return EventType.STREAM_STATUS;
    }

    @Override
    protected void serialize0(JsonObject json) {
        json.addProperty("is_live", live);
    }

}
