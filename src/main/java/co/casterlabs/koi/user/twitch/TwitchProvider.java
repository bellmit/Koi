package co.casterlabs.koi.user.twitch;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserProvider;
import lombok.NonNull;

// TODO c/p CaffeineProvider, and make it conform to Twitch.
public class TwitchProvider implements UserProvider {

    @Override
    public void hookWithAuth(@NonNull User user, @NonNull KoiAuthProvider auth) throws IdentifierException {
        // TODO Auto-generated method stub

    }

    @Override
    public void hook(@NonNull User user, @NonNull String username) throws IdentifierException {
        // TODO Auto-generated method stub

    }

}
