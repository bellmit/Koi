package co.casterlabs.koi.serialization;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import co.casterlabs.koi.user.User;

public class UserSerializer implements JsonSerializer<User> {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public JsonElement serialize(User user, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = GSON.toJsonTree(user).getAsJsonObject();

        result.addProperty("link", user.getPlatform().getLinkForUser(user.getUsername()));
        result.addProperty("UPID", user.getUUID() + ";" + user.getPlatform());

        return result;
    }

}
