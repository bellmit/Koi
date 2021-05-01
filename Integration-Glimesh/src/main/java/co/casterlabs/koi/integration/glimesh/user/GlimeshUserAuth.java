package co.casterlabs.koi.integration.glimesh.user;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserPlatform;

public class GlimeshUserAuth extends GlimeshAuth implements ClientAuthProvider {
    private static final String REDIRECT_URI = Koi.getInstance().getConfig().getGlimeshRedirectUri();
    private static final String CLIENT_SECRET = Koi.getInstance().getConfig().getGlimeshSecret();
    private static final String CLIENT_ID = Koi.getInstance().getConfig().getGlimeshId();

    private String token;
    private AuthData data;

    public GlimeshUserAuth(String token, AuthData data) throws ApiAuthException {
        super(data.refreshToken, REDIRECT_URI, CLIENT_ID, CLIENT_SECRET);

        this.token = token;
        this.data = data;

        this.update();
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
        throw new UnsupportedOperationException();
    }

}
