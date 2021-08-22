package co.casterlabs.koi.integration.glimesh.impl;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;

public class GlimeshUserAuth extends GlimeshAuth implements ClientAuthProvider {
    private static final String REDIRECT_URI = Koi.getInstance().getConfig().getGlimeshRedirectUri();
    private static final String CLIENT_SECRET = Koi.getInstance().getConfig().getGlimeshSecret();
    private static final String CLIENT_ID = Koi.getInstance().getConfig().getGlimeshId();

    private String token;
    private AuthData data;

    private SimpleProfile simpleProfile;

    public GlimeshUserAuth(String token, AuthData data) throws ApiAuthException, ApiException {
        super(data.refreshToken, REDIRECT_URI, CLIENT_ID, CLIENT_SECRET);

        this.token = token;
        this.data = data;

        this.update();

        this.simpleProfile = GlimeshProvider.getProfile(this).getSimpleProfile();
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.GLIMESH;
    }

    @Override
    public void refresh() throws ApiAuthException {
        super.refresh();
        this.update();
    }

    private void update() {
        if (this.data != null) {
            this.data.refreshToken = this.getRefreshToken();

            Natsukashii.update(this.token, this.data);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("authorization", "Bearer " + this.getAccessToken());
        payload.addProperty("client_id", CLIENT_ID);

        return payload;
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        return this.simpleProfile;
    }

}
