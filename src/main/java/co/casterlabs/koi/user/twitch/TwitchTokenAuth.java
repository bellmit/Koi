package co.casterlabs.koi.user.twitch;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixRefreshTokenAuth;

public class TwitchTokenAuth extends TwitchHelixRefreshTokenAuth implements KoiAuthProvider {

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TWITCH;
    }

    @Override
    public boolean isValid() {
        try {
            this.refresh();

            return true;
        } catch (ApiAuthException ignored) {
            return false;
        }
    }

    @Override
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("client-id", this.clientId);
        payload.addProperty("authorization", "Bearer " + this.accessToken);

        return payload;
    }

}
