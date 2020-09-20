package co.casterlabs.koi.user.twitch;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;

import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TwitchAuth implements AuthProvider {
    private @NonNull String username;
    private @NonNull String password;
    private @Getter @NonNull String clientId;

    public Twirk getTwirk(String username) throws IOException {
        return new TwirkBuilder("#" + username.toLowerCase(), this.username, this.password).build();
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        return Collections.singletonMap("Client-ID", this.clientId);
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
    public void refresh() throws Exception {
        // Unused
    }

    @Override
    public void sendChatMessage(User generic, String message) {
        TwitchUser user = (TwitchUser) generic;

        user.getMessages().getTwirk().channelMessage(message);
    }

}
