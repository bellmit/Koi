package co.casterlabs.koi.user.twitch;

import java.io.IOException;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixClientCredentialsAuth;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TwitchAuth extends TwitchHelixClientCredentialsAuth implements AuthProvider {
    private @NonNull String username;
    private @NonNull String password;

    public Twirk getTwirk(String username) throws IOException {
        return new TwirkBuilder("#" + username.toLowerCase(), this.username, this.password).build();
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
