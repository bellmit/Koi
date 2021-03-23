package co.casterlabs.koi;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.CaffeineAuth.CaffeineAuthResponse;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.clientid.ClientIdMeta;
import co.casterlabs.koi.clientid.ClientIdMismatchException;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.brime.BrimeUserAuth;
import co.casterlabs.koi.user.caffeine.CaffeineAuth;
import co.casterlabs.koi.user.glimesh.GlimeshUserAuth;
import co.casterlabs.koi.user.trovo.TrovoUserAuth;
import co.casterlabs.koi.user.twitch.TwitchTokenAuth;
import co.casterlabs.koi.util.WebUtil;
import co.casterlabs.trovoapi.TrovoScope;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Natsukashii {
    private static final List<String> TWITCH_SCOPES = Arrays.asList("user:read:email", "chat:read", "chat:edit", "bits:read", "channel:read:subscriptions", "channel_subscriptions", "channel:read:redemptions");
    private static final List<TrovoScope> TROVO_SCOPES = Arrays.asList(TrovoScope.CHANNEL_DETAILS_SELF, TrovoScope.CHAT_SEND_SELF, TrovoScope.SEND_TO_MY_CHANNEL, TrovoScope.USER_DETAILS_SELF, TrovoScope.CHAT_CONNECT);

    /* ---------------- */
    /* Auth             */
    /* ---------------- */

    public static void revoke(String token) {
        try {
            WebUtil.jsonSendHttpGet(Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/revoke", Collections.singletonMap("Authorization", "Bearer " + token), AuthResponse.class);
        } catch (Exception ignored) {}
    }

    public static void update(String token, AuthData data) {
        WebUtil.sendHttp(Koi.GSON.toJson(data), "PATCH", Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/update", Collections.singletonMap("Authorization", "Bearer " + token));
    }

    public static ClientAuthProvider get(String token, String clientId) throws AuthException, ClientIdMismatchException {
        try {
            AuthResponse response = WebUtil.jsonSendHttpGet(Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/data", Collections.singletonMap("Authorization", "Bearer " + token), AuthResponse.class);

            if (response.data == null) {
                throw new AuthException(response.errors);
            } else {
                // TODO In the future, clientId's will be required.
                if (response.data.clientId != null) {
                    if (!response.data.clientId.equals(clientId)) {
                        throw new ClientIdMismatchException();
                    }
                }

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

                    case GLIMESH:
                        if (Koi.getInstance().getConfig().isGlimeshEnabled()) {
                            return authGlimesh(token, response.data);
                        } else {
                            throw new AuthException("Glimesh support is disabled.");
                        }

                    case BRIME:
                        if (Koi.getInstance().getConfig().isBrimeEnabled()) {
                            return authBrime(response.data);
                        } else {
                            throw new AuthException("Brime support is disabled.");
                        }

                    case CASTERLABS_SYSTEM:
                        break;

                }

                throw new AuthException("Unsupported platform: " + response.data.platformType);
            }
        } catch (ClientIdMismatchException e) {
            throw e;
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);

            throw new AuthException(e.getMessage(), e);
        }
    }

    private static ClientAuthProvider authBrime(AuthData data) {
        return new BrimeUserAuth(data.refreshToken);
    }

    private static ClientAuthProvider authTrovo(AuthData data) throws ApiAuthException, AuthException {
        try {
            TrovoUserAuth auth = new TrovoUserAuth(data.refreshToken);

            if (auth.isValid()) {
                for (TrovoScope scope : TROVO_SCOPES) {
                    auth.checkScope(scope);
                }

                return auth;
            } else {
                throw new AuthException("Authorization invalid.");
            }
        } catch (ApiException | IOException e) {
            throw new ApiAuthException(e);
        }
    }

    private static ClientAuthProvider authGlimesh(String token, AuthData data) throws ApiAuthException, AuthException {
        return new GlimeshUserAuth(token, data);
    }

    private static ClientAuthProvider authTwitch(AuthData data) throws ApiAuthException, AuthException {
        if (data.scopes.containsAll(TWITCH_SCOPES)) {
            TwitchTokenAuth auth = new TwitchTokenAuth();

            auth.login(Koi.getInstance().getConfig().getTwitchSecret(), Koi.getInstance().getConfig().getTwitchId(), data.refreshToken);

            return auth;
        } else {
            throw new AuthException("Missing required scopes.");
        }
    }

    private static ClientAuthProvider authCaffeine(AuthData data) throws ApiAuthException, AuthException {
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
        private List<String> errors;
        private AuthData data;

    }

    public static class AuthData {
        @SerializedName("platform")
        public UserPlatform platformType;

        @SerializedName("_refresh_token")
        public String refreshToken;

        @SerializedName("client_id")
        public String clientId;

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

    /* ---------------- */
    /* Client ID        */
    /* ---------------- */

    @SuppressWarnings("deprecation")
    public static ClientIdMeta getClientIdMeta(String clientId) {
        String url = Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/thirdparty/data?client_id=" + URLEncoder.encode(clientId).replace("+", "%20");
        ClientIdResponse response = WebUtil.jsonSendHttpGet(url, null, ClientIdResponse.class);

        if (response.data == null) {
            return null;
        } else {
            return response.data.dataPayload.koi;
        }
    }

    @SuppressWarnings("deprecation")
    public static ClientIdMeta verifyClientId(String clientId, String secret) {
        String url = Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/thirdparty/verify?client_id=" + URLEncoder.encode(clientId).replace("+", "%20") + "&secret=" + URLEncoder.encode(secret).replace("+", "%20");
        ClientIdResponse response = WebUtil.jsonSendHttpGet(url, null, ClientIdResponse.class);

        if (response.data == null) {
            return null;
        } else {
            return response.data.dataPayload.koi;
        }
    }

    private static class ClientIdResponse {
        private ClientIdData data;

    }

    private static class ClientIdData {
        @SerializedName("data_payload")
        public ClientIdDataPayload dataPayload;

    }

    private static class ClientIdDataPayload {
        public ClientIdMeta koi;

    }

}
