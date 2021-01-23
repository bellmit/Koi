package co.casterlabs.koi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.CaffeineAuth.CaffeineAuthResponse;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.caffeine.CaffeineAuth;
import co.casterlabs.koi.user.trovo.TrovoUserAuth;
import co.casterlabs.koi.user.twitch.TwitchTokenAuth;
import co.casterlabs.koi.util.WebUtil;

public class Natsukashii {
    private static final List<String> TWITCH_SCOPES = Arrays.asList("user:read:email", "chat:read", "chat:edit");
    private static final List<String> TROVO_SCOPES = Arrays.asList("channel_details_self", "chat_send_self", "user_details_self", "chat_connect");

    public static void revoke(String token) {
        try {
            WebUtil.jsonSendHttpGet(Koi.getInstance().getConfig().getNatsukashiiEndpoint() + "/public/revoke", Collections.singletonMap("Authorization", "Bearer " + token), AuthResponse.class);
        } catch (Exception ignored) {}
    }

    public static KoiAuthProvider get(String token) throws AuthException {
        try {
            AuthResponse response = WebUtil.jsonSendHttpGet(Koi.getInstance().getConfig().getNatsukashiiEndpoint() + "/private/data", Collections.singletonMap("Authorization", "Bearer " + token), AuthResponse.class);

            if (response.data == null) {
                throw new AuthException(response.errors);
            } else {
                switch (response.data.platformType) {
                    case CAFFEINE:
                        if (Koi.getInstance().getConfig().isCaffeineEnabled()) {
                            return authCaffeine(response.data);
                        } else {
                            throw new AuthException("Caffeine support is disabled.");
                        }

                    case TWITCH:
                        if (Koi.getInstance().getConfig().isTwitchEnabled()) {
                            return authTwitch(response.data);
                        } else {
                            throw new AuthException("Twitch support is disabled.");
                        }

                    case TROVO:
                        if (Koi.getInstance().getConfig().isTrovoEnabled()) {
                            return authTrovo(response.data);
                        } else {
                            throw new AuthException("Twitch support is disabled.");
                        }

                    case CASTERLABS_SYSTEM:
                    default:
                        throw new AuthException("Unsupported platform: " + response.data.platformType);

                }
            }
        } catch (Exception e) {
            throw new AuthException(e.getMessage(), e);
        }
    }

    private static KoiAuthProvider authTrovo(AuthData data) throws ApiAuthException, AuthException {
        if (data.scopes.containsAll(TROVO_SCOPES)) {
            try {
                TrovoUserAuth auth = new TrovoUserAuth(data.refreshToken);

                if (auth.isValid()) {
                    return auth;
                } else {
                    throw new AuthException("Authorization invalid.");
                }
            } catch (ApiException | IOException e) {
                throw new ApiAuthException(e);
            }
        } else {
            throw new AuthException("Missing required scopes.");
        }
    }

    private static KoiAuthProvider authTwitch(AuthData data) throws ApiAuthException, AuthException {
        if (data.scopes.containsAll(TWITCH_SCOPES)) {
            TwitchTokenAuth auth = new TwitchTokenAuth();

            auth.login(Koi.getInstance().getConfig().getTwitchSecret(), Koi.getInstance().getConfig().getTwitchId(), data.refreshToken);

            return auth;
        } else {
            throw new AuthException("Missing required scopes.");
        }
    }

    private static KoiAuthProvider authCaffeine(AuthData data) throws ApiAuthException, AuthException {
        CaffeineAuth auth = new CaffeineAuth();

        CaffeineAuthResponse response = auth.login(data.refreshToken);

        switch (response) {
            case SUCCESS:
                return auth;

            case AWAIT2FA: // Not possible, but fall through.
            case INVALID:
            default:
                throw new AuthException("Refresh token is invalid.");

        }
    }

    private static class AuthResponse {
        public List<String> errors;
        public AuthData data;

    }

    private static class AuthData {
        @SerializedName("platform_type")
        public UserPlatform platformType;

        @SerializedName("_refresh_token")
        public String refreshToken;

        @SuppressWarnings("unused")
        public List<String> scopes;

    }

    public static class AuthException extends Exception {
        private static final long serialVersionUID = -3211377469215441818L;

        public AuthException(List<String> errors) {
            super(String.join(", ", errors));
        }

        public AuthException(String message) {
            super(message);
        }

        public AuthException(String message, Exception cause) {
            super(message, cause);
        }

    }

}
