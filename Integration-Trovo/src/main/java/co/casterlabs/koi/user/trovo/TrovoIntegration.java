package co.casterlabs.koi.user.trovo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.PlatformAuthorizer;
import co.casterlabs.koi.PlatformIntegration;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.trovo.data.TrovoUserConverter;
import co.casterlabs.koi.user.trovo.impl.TrovoAppAuth;
import co.casterlabs.koi.user.trovo.impl.TrovoProvider;
import co.casterlabs.koi.user.trovo.impl.TrovoUserAuth;
import co.casterlabs.trovoapi.TrovoScope;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class TrovoIntegration implements PlatformIntegration, PlatformAuthorizer {
    private static final List<TrovoScope> TROVO_SCOPES = Arrays.asList(TrovoScope.CHANNEL_DETAILS_SELF, TrovoScope.CHAT_SEND_SELF, TrovoScope.SEND_TO_MY_CHANNEL, TrovoScope.USER_DETAILS_SELF, TrovoScope.CHAT_CONNECT);

    private static @Getter TrovoIntegration instance;

    private @Getter TrovoProvider userProvider = new TrovoProvider();

    private @Getter TrovoAppAuth appAuth;

    public TrovoIntegration(KoiConfig config) throws ApiAuthException {
        instance = this;

        this.appAuth = new TrovoAppAuth(config.getTrovoId());

        FastLogger.logStatic("Enabled Trovo integration.");
    }

    @Override
    public ClientAuthProvider authorize(String token, AuthData data) throws ApiAuthException, ApiException {
        try {
            TrovoUserAuth auth = new TrovoUserAuth(data.refreshToken);

            if (auth.isValid()) {
                for (TrovoScope scope : TROVO_SCOPES) {
                    auth.checkScope(scope);
                }

                return auth;
            } else {
                throw new ApiAuthException("Authorization invalid.");
            }
        } catch (IOException e) {
            throw new ApiAuthException(e);
        }
    }

    @Override
    public UserConverter<?> getUserConverter() {
        return TrovoUserConverter.getInstance();
    }

    @Override
    public PlatformAuthorizer getPlatformAuthorizer() {
        return this;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TROVO;
    }

}
