package co.casterlabs.koi.user.twitch;

import java.io.IOException;
import java.util.logging.Level;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixClientCredentialsAuth;

public class TwitchAuth extends TwitchHelixClientCredentialsAuth implements AuthProvider {
    private String username;
    private String password;

    public TwitchAuth(String username, String password, String clientSecret, String clientId) throws ApiAuthException {
        this.username = username;
        this.password = password;

        this.login(clientSecret, clientId);
    }

    public Twirk getTwirk(String username) throws IOException {
        return new TwirkBuilder("#" + username.toLowerCase(), this.username, this.password).setVerbosity(Level.OFF).build();
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TWITCH;
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public void sendChatMessage(User generic, String message) {
        Koi.getMiscThreadPool().submit(() -> {
            TwitchUser user = (TwitchUser) generic;

            user.getMessages().getTwirk().channelMessage(message);
        });
    }

}
