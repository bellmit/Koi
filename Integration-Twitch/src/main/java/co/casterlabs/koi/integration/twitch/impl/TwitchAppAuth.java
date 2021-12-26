package co.casterlabs.koi.integration.twitch.impl;

import java.io.IOException;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixClientCredentialsAuth;
import lombok.Getter;

public class TwitchAppAuth extends TwitchHelixClientCredentialsAuth implements ClientAuthProvider {
    private @Getter String appUsername;
    private String appPassword;

    public TwitchAppAuth(String appUsername, String appPassword, String clientSecret, String clientId) throws ApiAuthException {
        this.appUsername = appUsername;
        this.appPassword = appPassword;
        this.login(clientSecret, clientId);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TWITCH;
    }

    @Override
    public JsonObject getCredentials() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        throw new UnsupportedOperationException();
    }

    public Twirk getTwirk(String username) throws IOException {
        return new TwirkBuilder(username.toLowerCase(), this.appUsername, this.appPassword)
            .setInfoLogMethod(null)
            .setWarningLogMethod(TwitchTokenAuth.twirkLogger::warn)
            .setErrorLogMethod(TwitchTokenAuth.twirkLogger::severe)
            .setDebugLogMethod(null)
            .setPingInterval(60)
            .build();
    }

}
