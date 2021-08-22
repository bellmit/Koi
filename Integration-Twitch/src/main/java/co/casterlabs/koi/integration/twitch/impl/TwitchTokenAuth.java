package co.casterlabs.koi.integration.twitch.impl;

import java.io.IOException;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixRefreshTokenAuth;
import co.casterlabs.twitchapi.helix.requests.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.types.HelixUser;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class TwitchTokenAuth extends TwitchHelixRefreshTokenAuth implements ClientAuthProvider {
    private static final FastLogger logger = new FastLogger("Java-Twirk");

    private SimpleProfile simpleProfile;

    @Override
    public TwitchHelixRefreshTokenAuth login(@NonNull String clientSecret, @NonNull String clientId, @NonNull String refreshToken) throws ApiAuthException {
        super.login(clientSecret, clientId, refreshToken);

        try {
            HelixGetUsersRequest request = new HelixGetUsersRequest(this);

            HelixUser profile = request.send().get(0);
            User asUser = TwitchUserConverter.transform(profile);

            this.simpleProfile = asUser.getSimpleProfile();

            return this;
        } catch (ApiException e) {
            throw new ApiAuthException(e);
        }
    }

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
        return new TwirkBuilder(username.toLowerCase(), username, "oauth:" + this.accessToken)
            .setInfoLogMethod(null)
            .setWarningLogMethod(logger::warn)
            .setErrorLogMethod(logger::severe)
            .setDebugLogMethod(null)
            .setPingInterval(60)
            .build();
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        return this.simpleProfile;
    }

}
