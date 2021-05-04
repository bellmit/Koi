package co.casterlabs.koi.serialization;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.config.BadgeConfiguration;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;

public class UserSerializer implements JsonSerializer<User> {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public JsonElement serialize(User user, Type typeOfSrc, JsonSerializationContext context) {
        BadgeConfiguration badges = Koi.getInstance().getForcedBadges(user.getPlatform(), user.getId());

        if (badges != null) {
            if (badges.isIgnoreExisting()) {
                user.getBadges().clear();
            }

            user.getBadges().addAll(badges.getBadges());
        }

        JsonObject result = GSON.toJsonTree(user).getAsJsonObject();

        if (user.getPlatform() == UserPlatform.TWITCH) {
            result.addProperty("link", user.getPlatform().getLinkForUser(user.getUsername()));
        } else {
            result.addProperty("link", user.getPlatform().getLinkForUser(user.getDisplayname()));
        }

        result.addProperty("UPID", user.getId() + ";" + user.getPlatform());
        result.addProperty("UUID", user.getId()); // TODO Deprecate

        return result;
    }

}
