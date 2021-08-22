package co.casterlabs.koi.integration.glimesh;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.PlatformAuthorizer;
import co.casterlabs.koi.PlatformIntegration;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.integration.glimesh.data.GlimeshUserConverter;
import co.casterlabs.koi.integration.glimesh.impl.GlimeshApplicationAuth;
import co.casterlabs.koi.integration.glimesh.impl.GlimeshProvider;
import co.casterlabs.koi.integration.glimesh.impl.GlimeshUserAuth;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class GlimeshIntegration implements PlatformIntegration, PlatformAuthorizer {

    private static @Getter GlimeshIntegration instance;

    private @Getter GlimeshProvider userProvider = new GlimeshProvider();

    private @Getter GlimeshApplicationAuth appAuth;

    public GlimeshIntegration(KoiConfig config) throws ApiAuthException {
        instance = this;

        this.appAuth = new GlimeshApplicationAuth(config.getGlimeshId());

        FastLogger.logStatic("Enabled Glimesh integration.");
    }

    @Override
    public ClientAuthProvider authorize(String token, AuthData data) throws ApiAuthException, ApiException {
        return new GlimeshUserAuth(token, data);
    }

    @Override
    public UserConverter<?> getUserConverter() {
        return GlimeshUserConverter.getInstance();
    }

    @Override
    public PlatformAuthorizer getPlatformAuthorizer() {
        return this;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.GLIMESH;
    }

}
