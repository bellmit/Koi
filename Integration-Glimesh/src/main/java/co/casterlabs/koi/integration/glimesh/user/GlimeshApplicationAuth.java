package co.casterlabs.koi.integration.glimesh.user;

import com.google.gson.JsonObject;

import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;

public class GlimeshApplicationAuth extends GlimeshAuth implements ClientAuthProvider {

    public GlimeshApplicationAuth(String clientId) {
        super(clientId);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.GLIMESH;
    }

    @Override
    public boolean isValid() {
        return true;
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
