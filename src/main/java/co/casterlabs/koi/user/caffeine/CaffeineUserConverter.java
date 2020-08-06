package co.casterlabs.koi.user.caffeine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPreferences;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;

public class CaffeineUserConverter implements UserConverter<JsonObject> {
    
    private static @Getter UserConverter<JsonObject> instance = new CaffeineUserConverter();

    @Override
    public JsonObject transform(JsonObject user) {
        JsonObject json = new JsonObject();
        JsonElement nameJson = user.get("name");
        String displayname = (nameJson.isJsonNull()) ? user.get("username").getAsString() : nameJson.getAsString();
        UserPreferences preferences = UserPreferences.get(UserPlatform.CAFFEINE, user.get("caid").getAsString());
        
        json.add("UUID", user.get("caid"));
        json.addProperty("displayname", displayname);
        json.add("username", user.get("username"));
        json.addProperty("image_link", CaffeineLinks.getAvatarLink(user.get("avatar_image_path").getAsString()));
        json.add("follower_count", user.get("followers_count"));
        json.add("following_count", user.get("following_count"));
        json.addProperty("platform", UserPlatform.CAFFEINE.name());
        json.addProperty("color", preferences.getColor());

        return json;
    }

    @Override
    public JsonObject get(String UUID) throws IdentifierException {
        JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getUsersLink(UUID), null, JsonObject.class);

        if (json.has("errors")) {
            throw new IdentifierException();
        }
        
        return this.transform(json.get("user").getAsJsonObject());
    }
    
}
