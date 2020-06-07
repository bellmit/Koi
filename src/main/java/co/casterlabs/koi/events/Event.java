package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.networking.JsonSerializer;
import co.casterlabs.koi.user.User;
import lombok.Getter;

@Getter
public abstract class Event implements JsonSerializer {
    protected User streamer;

    @Override
    public JsonObject serialize() {
        JsonObject json = new JsonObject();

        json.addProperty("event_type", this.getType().name());
        json.add("streamer", this.streamer.serialize());

        this.serialize0(json);

        return json;
    }

    protected abstract void serialize0(JsonObject json);

    public abstract EventType getType();

}
