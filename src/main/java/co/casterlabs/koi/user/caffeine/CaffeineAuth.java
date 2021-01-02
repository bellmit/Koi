package co.casterlabs.koi.user.caffeine;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;

public class CaffeineAuth extends co.casterlabs.caffeineapi.CaffeineAuth implements KoiAuthProvider {

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.CAFFEINE;
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("authorization", "Bearer " + this.getAccessToken());
        payload.addProperty("signed", this.getSignedToken());
        payload.addProperty("credential", this.getCredential());

        return payload;
    }

}
