package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import lombok.Getter;
import lombok.Setter;

public abstract class Event {
    private @Getter @Setter boolean upvotable = false;

    public abstract User getStreamer();

    public abstract EventType getType();

    public JsonObject serialize() {
        JsonObject json = Koi.GSON.toJsonTree(this).getAsJsonObject();

        json.addProperty("event_type", this.getType().name());
        json.addProperty("upvotable", this.upvotable);

        return json;
    }

    @Override
    public String toString() {
        return this.serialize().toString();
    }

}
