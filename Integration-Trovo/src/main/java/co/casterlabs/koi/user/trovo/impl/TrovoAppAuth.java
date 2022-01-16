package co.casterlabs.koi.user.trovo.impl;

import com.google.gson.JsonObject;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;
import lombok.NonNull;

@SuppressWarnings("deprecation")
public class TrovoAppAuth extends co.casterlabs.trovoapi.TrovoApplicationAuth implements ClientAuthProvider {

    public TrovoAppAuth(@NonNull String clientId) {
        super(clientId);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TROVO;
    }

    @Override
    public JsonObject getCredentials() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        throw new UnsupportedOperationException();
    }

}
