package co.casterlabs.koi.user.twitch;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixClientCredentialsAuth;

@SuppressWarnings("unused")
public class TwitchCredentialsAuth extends TwitchHelixClientCredentialsAuth implements KoiAuthProvider {
    private String username;
    private String password;

    public TwitchCredentialsAuth(String username, String password, String clientSecret, String clientId) throws ApiAuthException {
        this.username = username;
        this.password = password;

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
