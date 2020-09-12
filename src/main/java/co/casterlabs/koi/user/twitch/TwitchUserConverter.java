package co.casterlabs.koi.user.twitch;

import com.gikk.twirk.types.users.TwitchUser;
import com.google.gson.JsonObject;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter UserConverter<com.gikk.twirk.types.users.TwitchUser> instance = new TwitchUserConverter();

    @Override
    public SerializedUser transform(TwitchUser user) {
        SerializedUser result = new SerializedUser(UserPlatform.TWITCH);

        result.setDisplayname(user.getDisplayName());
        result.setUUID(String.valueOf(user.getUserID()));
        result.setUsername(user.getDisplayName());
        result.setColor("#" + Integer.toHexString(user.getColor()).toUpperCase());
        // TODO image link

        return result;
    }

    @Override
    public SerializedUser get(String UUID) throws IdentifierException {
        JsonObject data = null;

        JsonObject json = WebUtil.jsonSendHttpGet(TwitchLinks.getUserByLoginLink(UUID), null, JsonObject.class);

        if (json.get("_total").getAsInt() != 0) {
            data = json.getAsJsonArray("users").get(0).getAsJsonObject();
        } else {
            data = WebUtil.jsonSendHttpGet(TwitchLinks.getUserByIdLink(UUID), null, JsonObject.class);

            if (data.has("error")) {
                throw new IdentifierException();
            }
        }

        if (data == null) {
            throw new IdentifierException();
        }

        SerializedUser result = new SerializedUser(UserPlatform.TWITCH);

        result.setDisplayname(data.get("display_name").getAsString());
        result.setUsername(data.get("display_name").getAsString());
        result.setUUID(data.get("_id").getAsString());
        result.setImageLink(data.get("logo").getAsString());

        return result;
    }

    @Data
    @Accessors(chain = true)
    public static class UserImageHolder {
        private String image;
        private long lastUpdated;

        public void check() {

        }

    }

}
