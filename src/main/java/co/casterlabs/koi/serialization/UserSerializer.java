package co.casterlabs.koi.serialization;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import co.casterlabs.koi.user.PolyFillRequirements;
import co.casterlabs.koi.user.User;

public class UserSerializer implements JsonSerializer<User> {

    @Override
    public JsonElement serialize(User user, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();

        json.addProperty("UUID", user.getUUID());
        json.addProperty("displayname", user.getDisplayname());
        json.addProperty("username", user.getUsername());
        json.addProperty("image_link", user.getImageLink());
        json.addProperty("follower_count", user.getFollowerCount());
        json.addProperty("platform", user.getPlatform().name());
        json.addProperty("link", user.getPlatform().getLinkForUser(user.getUsername()));
        json.addProperty("color", user.getPreferences().get(PolyFillRequirements.COLOR));

        return json;
    }

}
