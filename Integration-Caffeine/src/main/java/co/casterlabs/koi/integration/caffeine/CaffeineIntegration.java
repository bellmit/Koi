package co.casterlabs.koi.integration.caffeine;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.PlatformAuthorizer;
import co.casterlabs.koi.PlatformIntegration;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.integration.caffeine.data.CaffeineUserConverter;
import co.casterlabs.koi.integration.caffeine.impl.CaffeineIntegrationAuth;
import co.casterlabs.koi.integration.caffeine.impl.CaffeineProvider;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class CaffeineIntegration implements PlatformIntegration, PlatformAuthorizer {

    private static @Getter CaffeineIntegration instance;

    private @Getter CaffeineProvider userProvider = new CaffeineProvider();

    public CaffeineIntegration(KoiConfig config) {
        instance = this;

        FastLogger.logStatic("Enabled Caffeine integration.");
    }

    @Override
    public ClientAuthProvider authorize(String token, AuthData data) throws ApiAuthException, ApiException {
        CaffeineIntegrationAuth auth = new CaffeineIntegrationAuth();

        auth.login(data.refreshToken);

        return auth;
    }

    @Override
    public ClientAuthProvider getAppAuth() {
        return null;
    }

    @Override
    public UserConverter<?> getUserConverter() {
        return CaffeineUserConverter.getInstance();
    }

    @Override
    public PlatformAuthorizer getPlatformAuthorizer() {
        return this;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.CAFFEINE;
    }

}
