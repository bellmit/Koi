package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class ChatEvent extends Event {
    private String id;
    private String message;
    private JsonObject sender;
    private User streamer;

    @Override
    public EventType getType() {
        return EventType.CHAT;
    }

}
