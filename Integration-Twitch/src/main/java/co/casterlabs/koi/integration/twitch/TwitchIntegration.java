package co.casterlabs.koi.integration.twitch;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.Natsukashii.AuthData;
import co.casterlabs.koi.PlatformAuthorizer;
import co.casterlabs.koi.PlatformIntegration;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
import co.casterlabs.koi.integration.twitch.impl.TwitchAppAuth;
import co.casterlabs.koi.integration.twitch.impl.TwitchProvider;
import co.casterlabs.koi.integration.twitch.impl.TwitchTokenAuth;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.twitchapi.helix.CheermoteCache;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class TwitchIntegration implements PlatformIntegration, PlatformAuthorizer {
    private static final List<String> TWITCH_SCOPES = Arrays.asList("user:read:email", "chat:read", "chat:edit", "bits:read", "channel:read:subscriptions", "channel_subscriptions", "channel:read:redemptions");

    private static @Getter TwitchIntegration instance;

    private @Getter TwitchProvider userProvider = new TwitchProvider();

    private @Getter TwitchAppAuth appAuth;

    @SuppressWarnings("resource")
    public TwitchIntegration(KoiConfig config) throws ApiAuthException {
        instance = this;

        this.appAuth = new TwitchAppAuth(config.getTwitchSecret(), config.getTwitchId());

        new RepeatingThread("Twitch Cheermote Refresh - Koi", TimeUnit.HOURS.toMillis(1), () -> {
            try {
                CheermoteCache.update(this.appAuth).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        FastLogger.logStatic("Enabled Twitch integration.");
    }

    @Override
    public ClientAuthProvider authorize(String token, AuthData data) throws ApiAuthException, ApiException {
        if (data.scopes.containsAll(TWITCH_SCOPES)) {
            TwitchTokenAuth auth = new TwitchTokenAuth();

            auth.login(Koi.getInstance().getConfig().getTwitchSecret(), Koi.getInstance().getConfig().getTwitchId(), data.refreshToken);

            return auth;
        } else {
            throw new ApiAuthException("Missing required scopes.");
        }
    }

    @Override
    public UserConverter<?> getUserConverter() {
        return TwitchUserConverter.getInstance();
    }

    @Override
    public PlatformAuthorizer getPlatformAuthorizer() {
        return this;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.TWITCH;
    }

}
