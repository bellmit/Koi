package co.casterlabs.koi.user.caffeine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPreferences;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;

public class CaffeineUserConverter implements UserConverter<JsonObject> {
    private static @Getter UserConverter<JsonObject> instance = new CaffeineUserConverter();

    @Override
    public SerializedUser transform(JsonObject user) {
        JsonElement nameJson = user.get("name");
        String displayname = (nameJson.isJsonNull()) ? user.get("username").getAsString() : nameJson.getAsString();
        UserPreferences preferences = UserPreferences.get(UserPlatform.CAFFEINE, user.get("caid").getAsString());
        SerializedUser result = new SerializedUser(UserPlatform.CAFFEINE);

        result.setUUID(user.get("caid").getAsString());
        result.setDisplayname(displayname);
        result.setUsername(user.get("username").getAsString());
        result.setImageLink(CaffeineLinks.getAvatarLink(user.get("avatar_image_path").getAsString()));
        result.setColor(preferences.getColor());

        return result;
    }

    @Override
    public SerializedUser get(String UUID) throws IdentifierException {
        JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getUsersLink(UUID), null, JsonObject.class);

        if (json.has("errors")) {
            throw new IdentifierException();
        }

        return this.transform(json.get("user").getAsJsonObject());
    }

}
