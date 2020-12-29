package co.casterlabs.koi.user.twitch;

import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixRefreshTokenAuth;

public class TwitchTokenAuth extends TwitchHelixRefreshTokenAuth implements KoiAuthProvider {

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TWITCH;
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

}
