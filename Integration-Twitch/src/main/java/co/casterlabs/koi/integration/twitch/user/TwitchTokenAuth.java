package co.casterlabs.koi.integration.twitch.user;

import java.io.IOException;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixRefreshTokenAuth;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class TwitchTokenAuth extends TwitchHelixRefreshTokenAuth implements ClientAuthProvider {
    private static final FastLogger logger = new FastLogger("Java-Twirk");

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

        payload.addProperty("client_id", this.clientId);
        payload.addProperty("authorization", "Bearer " + this.accessToken);

        return payload;
    }

    public Twirk getTwirk(String username) throws IOException {
        //@formatter:off
        return new TwirkBuilder(username.toLowerCase(), username, "oauth:" + this.accessToken)
                .setInfoLogMethod(null)
                .setWarningLogMethod(logger::warn)
                .setErrorLogMethod(logger::severe)
                .setDebugLogMethod(null)
                .setPingInterval(60)
                .build();
        //@formatter:on
    }

}
