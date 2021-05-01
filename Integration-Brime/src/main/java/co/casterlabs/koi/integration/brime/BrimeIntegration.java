package co.casterlabs.koi.integration.brime;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.brimeapijava.BrimeApi;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.PlatformAuthorizer;
import co.casterlabs.koi.PlatformIntegration;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.integration.brime.user.BrimeAppAuth;
import co.casterlabs.koi.integration.brime.user.BrimeProvider;
import co.casterlabs.koi.integration.brime.user.BrimeUserAuth;
import co.casterlabs.koi.integration.brime.user.BrimeUserConverter;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class BrimeIntegration implements PlatformIntegration, PlatformAuthorizer {

    private static @Getter BrimeIntegration instance;

    private @Getter BrimeProvider userProvider = new BrimeProvider();

    private @Getter BrimeAppAuth appAuth;
    private @Getter String ablySecret;
    private @Getter String clientId;

    public BrimeIntegration(KoiConfig config) throws ApiAuthException {
        instance = this;

        BrimeApi.targetApiEndpoint = BrimeApi.STAGING_API;

        this.appAuth = new BrimeAppAuth(config.getBrimeClientId());
        this.ablySecret = config.getBrimeAblySecret();
        this.clientId = config.getBrimeClientId();

        FastLogger.logStatic("Enabled Brime integration.");
    }

    @Override
    public ClientAuthProvider authorize(String token, AuthData data) throws ApiAuthException, ApiException {
        return new BrimeUserAuth(this.clientId, data.refreshToken);
    }

    @Override
    public UserConverter<?> getUserConverter() {
        return BrimeUserConverter.getInstance();
    }

    @Override
    public PlatformAuthorizer getPlatformAuthorizer() {
        return this;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.BRIME;
    }

}
