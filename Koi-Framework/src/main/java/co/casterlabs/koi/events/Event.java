package co.casterlabs.koi.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import lombok.Data;

public abstract class Event {
    public abstract User getStreamer();

    public abstract EventType getType();

    @SerializedName("event_abilities")
    public final EventAbilities abilities = new EventAbilities();

    public JsonObject serialize() {
        JsonObject json = Koi.GSON.toJsonTree(this).getAsJsonObject();

        json.addProperty("event_type", this.getType().name());

        return json;
    }

    @Override
    public String toString() {
        return this.serialize().toString();
    }

    @Data
    public static class EventAbilities {
        private boolean upvotable = false;
        private boolean deletable = false;

    }

}
