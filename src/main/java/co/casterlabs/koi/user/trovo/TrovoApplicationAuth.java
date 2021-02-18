package co.casterlabs.koi.user.trovo;

import com.google.gson.JsonObject;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import lombok.NonNull;

public class TrovoApplicationAuth extends co.casterlabs.trovoapi.TrovoApplicationAuth implements ClientAuthProvider {

    public TrovoApplicationAuth(@NonNull String clientId) {
        super(clientId);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TROVO;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public JsonObject getCredentials() {
        throw new UnsupportedOperationException();
    }

}
