package co.casterlabs.koi.user.caffeine;

import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;

public class CaffeineAuth extends co.casterlabs.caffeineapi.CaffeineAuth implements KoiAuthProvider {

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.CAFFEINE;
    }

    @Override
    public boolean isLoggedIn() {
        return this.isValid();
    }

}
