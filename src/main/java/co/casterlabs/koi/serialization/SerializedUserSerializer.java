package co.casterlabs.koi.serialization;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import co.casterlabs.koi.user.SerializedUser;

// TODO better class name
public class SerializedUserSerializer implements JsonSerializer<SerializedUser> {
    private static final Gson GSON = new Gson();

    @Override
    public JsonElement serialize(SerializedUser src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = GSON.toJsonTree(src).getAsJsonObject();

        result.addProperty("link", src.getPlatform().getLinkForUser(src.getUsername()));

        return result;
    }

}
