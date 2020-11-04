package co.casterlabs.koi.serialization;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import co.casterlabs.koi.user.PolyFillRequirements;
import co.casterlabs.koi.user.User;

public class UserSerializer implements JsonSerializer<User> {
    private static final Gson GSON = new Gson();

    @Override
    public JsonElement serialize(User user, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        List<String> badges = new ArrayList<>();

        badges.addAll(user.getPreferences().getForcedBadges());
        badges.addAll(user.getBadges());

        json.addProperty("UUID", user.getUUID());
        json.addProperty("displayname", user.getDisplayname());
        json.addProperty("username", user.getUsername());
        json.add("badges", GSON.toJsonTree(badges));
        json.addProperty("image_link", user.getImageLink());
        json.addProperty("follower_count", user.getFollowerCount());
        json.addProperty("platform", user.getPlatform().name());
        json.addProperty("link", user.getPlatform().getLinkForUser(user.getUsername()));
        json.addProperty("color", user.getPreferences().get(PolyFillRequirements.COLOR));

        return json;
    }

}
