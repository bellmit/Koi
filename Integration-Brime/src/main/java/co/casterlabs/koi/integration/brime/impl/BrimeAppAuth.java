package co.casterlabs.koi.integration.brime.impl;

import com.google.gson.JsonObject;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class BrimeAppAuth extends co.casterlabs.brimeapijava.BrimeApplicationAuth implements ClientAuthProvider {

    public BrimeAppAuth(@NonNull String clientId) {
        super(clientId);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.BRIME;
    }

    @Override
    public boolean isValid() {
        return true; // No way to test PEPEGACLAP
    }

    @Override
    public void refresh() {
        throw new UnsupportedOperationException();
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
