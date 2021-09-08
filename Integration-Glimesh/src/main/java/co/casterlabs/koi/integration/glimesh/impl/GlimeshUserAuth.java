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
    private String token;
    private AuthData data;

    private SimpleProfile simpleProfile;

    public GlimeshUserAuth(String token, AuthData data) throws ApiAuthException, ApiException {
        super(
            data.refreshToken,
            Koi.getInstance().getConfig().getGlimeshRedirectUri(),
            Koi.getInstance().getConfig().getGlimeshId(),
            Koi.getInstance().getConfig().getGlimeshSecret()
        );

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
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("authorization", "Bearer " + this.getAccessToken());
        payload.addProperty("client_id", Koi.getInstance().getConfig().getGlimeshId());

        return payload;
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        return this.simpleProfile;
    }

}
