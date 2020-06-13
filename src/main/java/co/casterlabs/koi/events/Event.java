package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;

public abstract class Event {

    public JsonObject serialize() {
        JsonObject json = Koi.GSON.toJsonTree(this).getAsJsonObject();

        json.addProperty("event_type", this.getType().name());

        return json;
    }
    
    public abstract User getStreamer();

    public abstract EventType getType();

    @Override
    public String toString() {
        return this.serialize().toString();
    }

}
