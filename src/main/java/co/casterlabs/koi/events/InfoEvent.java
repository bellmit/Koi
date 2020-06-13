package co.casterlabs.koi.events;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class InfoEvent extends Event {
    private User streamer;
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

    @SneakyThrows
    public static InfoEvent fromJson(JsonObject json, User streamer) {
        JsonObject event = json.getAsJsonObject("event");
        EventType type = EventType.valueOf(event.get("event_type").getAsString());
        
        // This updates the other user with the most current information.
        JsonObject otherUser = event.getAsJsonObject(type.getOtherUser());
        UserPlatform otherPlatform = UserPlatform.valueOf(otherUser.get("platform").getAsString());
        User other = otherPlatform.getUser(otherUser.get("UUID").getAsString());

        event.add("streamer", Koi.GSON.toJsonTree(streamer, new TypeToken<User>() {}.getType()));
        event.add(type.getOtherUser(), Koi.GSON.toJsonTree(other, new TypeToken<User>() {}.getType()));
        
        return new InfoEvent(streamer, event);
    }

}
