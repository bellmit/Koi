package co.casterlabs.koi.integration.brime;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.PlatformAuthorizer;
import co.casterlabs.koi.PlatformIntegration;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.integration.brime.data.BrimeUserConverter;
import co.casterlabs.koi.integration.brime.impl.BrimeProvider;
import co.casterlabs.koi.integration.brime.impl.BrimeUserAuth;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@SuppressWarnings("deprecation")
public class BrimeIntegration implements PlatformIntegration, PlatformAuthorizer {

    private static @Getter BrimeIntegration instance;

    private @Getter BrimeProvider userProvider = new BrimeProvider();

    private @Getter String clientId;
    private @Getter String clientSecret;

    public BrimeIntegration(KoiConfig config) throws ApiAuthException {
        instance = this;

        this.clientId = config.getBrimeId();
        this.clientSecret = config.getBrimeSecret();

        if (config.isBrimeBetterBrimeEnabled()) {
            new BetterBrimeEmoteProvider();
        }

        FastLogger.logStatic("Enabled Brime integration.");
    }

    @Override
    public ClientAuthProvider authorize(String token, AuthData data) throws ApiAuthException, ApiException {
        return new BrimeUserAuth(data.refreshToken, this.clientId, this.clientSecret);
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

    @Override
    public ClientAuthProvider getAppAuth() {
        return null;
    }

}
