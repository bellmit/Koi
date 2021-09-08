package co.casterlabs.koi.integration.caffeine.impl;

import com.google.gson.JsonObject;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;

public class CaffeineIntegrationAuth extends co.casterlabs.caffeineapi.CaffeineAuth implements ClientAuthProvider {

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.CAFFEINE;
    }

    @Override
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("authorization", "Bearer " + this.getAccessToken());
        payload.addProperty("signed", this.getSignedToken());
        payload.addProperty("credential", this.getCredential());

        return payload;
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        return new SimpleProfile(this.getCaid(), this.getCaid(), UserPlatform.CAFFEINE);
    }

}
