package co.casterlabs.koi.user.caffeine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest.UserBadge;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PolyFillRequirements;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPolyFill;
import lombok.Getter;

public class CaffeineUserConverter implements UserConverter<JsonObject> {
    private static @Getter UserConverter<JsonObject> instance = new CaffeineUserConverter();

    @Override
    public SerializedUser transform(JsonObject user) {
        JsonElement nameJson = user.get("name");
        String displayname = (nameJson.isJsonNull()) ? user.get("username").getAsString() : nameJson.getAsString();
        UserPolyFill preferences = UserPolyFill.get(UserPlatform.CAFFEINE, user.get("caid").getAsString());
        SerializedUser result = new SerializedUser(UserPlatform.CAFFEINE);
        UserBadge badge = UserBadge.from(user.get("badge"));

        if (badge != UserBadge.NONE) {
            result.getBadges().add(badge.getImageLink());
        }

        result.setUUID(user.get("caid").getAsString());
        result.setDisplayname(displayname);
        result.setUsername(user.get("username").getAsString());
        result.setImageLink(CaffeineLinks.getAvatarLink(user.get("avatar_image_path").getAsString()));
        result.setColor(preferences.get(PolyFillRequirements.COLOR));

        return result;
    }

    @Override
    public SerializedUser get(String UUID) throws IdentifierException {
        SerializedUser result = new SerializedUser(UserPlatform.CAFFEINE);
        CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

        request.setCAID(UUID);

        co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest.CaffeineUser data = null;

        try {
            data = request.send();
        } catch (Exception e) {
            throw new IdentifierException();
        }

        UserPolyFill preferences = UserPolyFill.get(UserPlatform.CAFFEINE, data.getCAID());

        result.setUUID(data.getCAID());
        result.setUsername(data.getUsername());
        result.setImageLink(data.getImageLink());
        result.setDisplayname(((data.getDisplayname() == null) || data.getDisplayname().isEmpty()) ? data.getUsername() : data.getDisplayname());
        result.setColor(preferences.get(PolyFillRequirements.COLOR));

        return result;
    }

}
