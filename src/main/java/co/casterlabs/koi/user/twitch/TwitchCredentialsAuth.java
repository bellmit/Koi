package co.casterlabs.koi.user.twitch;

import java.io.IOException;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixClientCredentialsAuth;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class TwitchCredentialsAuth extends TwitchHelixClientCredentialsAuth implements KoiAuthProvider {
    private static final FastLogger logger = new FastLogger("Java-Twirk");

    private String username;
    private String password;

    public TwitchCredentialsAuth(String username, String password, String clientSecret, String clientId) throws ApiAuthException {
        this.username = username;
        this.password = password;

        this.login(clientSecret, clientId);
    }

    public Twirk getTwirk(String username) throws IOException {
        //@formatter:off
        return new TwirkBuilder(username.toLowerCase(), this.username, this.password)
                .setInfoLogMethod(null)
                .setWarningLogMethod(logger::warn)
                .setErrorLogMethod(logger::severe)
                .setDebugLogMethod(null)
                .setPingInterval(60)
                .build();
        //@formatter:on
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
