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
public class ShareEvent extends Event {
    private String message;
    private User sender;

    @SneakyThrows
    public ShareEvent(String message, User sender, User streamer) {
        this.streamer = streamer;
        this.sender = sender;
        this.message = message;
    }

    @Override
    public EventType getType() {
        return EventType.SHARE;
    }

    @Override
    protected void serialize0(JsonObject json) {
        json.addProperty("message", this.message);
        json.add("sender", this.sender.serialize());
    }

}
