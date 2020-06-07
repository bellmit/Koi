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
public class InfoEvent extends Event {
    private JsonObject event;

    @SneakyThrows
    public InfoEvent(Event event) {
        this.streamer = event.getStreamer();
        this.event = event.serialize();
    }

    @SneakyThrows
    public InfoEvent(User streamer, JsonObject json) {
        this.streamer = streamer;
        this.event = json;
    }

    @Override
    public EventType getType() {
        return EventType.INFO;
    }

    @Override
    protected void serialize0(JsonObject json) {
        json.add("event", this.event);
    }

    @SneakyThrows
    public static InfoEvent fromJson(JsonObject json, User streamer) {
        JsonObject event = json.getAsJsonObject("event").getAsJsonObject("event");

        return new InfoEvent(streamer, event);
    }

}
