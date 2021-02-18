package co.casterlabs.koi.user.twitch;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixClientCredentialsAuth;

public class TwitchCredentialsAuth extends TwitchHelixClientCredentialsAuth implements ClientAuthProvider {

    public TwitchCredentialsAuth(String clientSecret, String clientId) throws ApiAuthException {
        this.login(clientSecret, clientId);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TWITCH;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public JsonObject getCredentials() {
        throw new IllegalStateException("Client Credentials cannot have an auth payload.");
    }

}
