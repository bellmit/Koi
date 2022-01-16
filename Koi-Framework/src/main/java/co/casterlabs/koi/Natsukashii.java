package co.casterlabs.koi;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.clientid.ClientIdMeta;
import co.casterlabs.koi.clientid.ClientIdMismatchException;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.ToString;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@SuppressWarnings("deprecation")
public class Natsukashii {

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

    public static ClientAuthProvider get(String token, String clientId) throws ApiAuthException, ClientIdMismatchException {
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

                FastLogger.logStatic(LogLevel.TRACE, "Auth payload: %s", response.data);

                PlatformAuthorizer authorizer = Koi.getInstance().getPlatformAuthorizer(response.data.platformType);

                if (authorizer == null) {
                    throw new AuthException("Unsupported platform: " + response.data.platformType);
                } else {
                    return authorizer.authorize(token, response.data);
                }
            }
        } catch (ClientIdMismatchException e) {
            throw e;
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);

            throw new ApiAuthException(e.getMessage(), e);
        }
    }

    private static class AuthResponse {
        private List<String> errors;
        private AuthData data;

    }

    @ToString
    public static class AuthData {
        @SerializedName("platform")
        public UserPlatform platformType;

        @SerializedName("_refresh_token")
        public String refreshToken;

        @SerializedName("client_id")
        public String clientId;

        public List<String> scopes;

    }

    private static class AuthException extends Exception {
        private static final long serialVersionUID = -3211377469215441818L;

        public AuthException(List<String> errors) {
            super(String.join(", ", errors));
        }

        public AuthException(String message) {
            super(message);
        }

    }

    /* ---------------- */
    /* Client ID        */
    /* ---------------- */

    public static ClientIdMeta getClientIdMeta(String clientId) {
        String url = Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/thirdparty/data?client_id=" + URLEncoder.encode(clientId).replace("+", "%20");
        ClientIdData response = WebUtil.jsonSendHttpGet(url, null, ClientIdData.class);

        if ((response == null) || (response.dataPayload == null)) {
            return null;
        } else {
            return response.dataPayload.koi;
        }
    }

    public static ClientIdMeta verifyClientId(String clientId, String secret) {
        String url = Koi.getInstance().getConfig().getNatsukashiiPrivateEndpoint() + "/thirdparty/verify?client_id=" + URLEncoder.encode(clientId).replace("+", "%20") + "&secret=" + URLEncoder.encode(secret).replace("+", "%20");
        ClientIdData response = WebUtil.jsonSendHttpGet(url, null, ClientIdData.class);

        if (response.dataPayload == null) {
            return null;
        } else {
            return response.dataPayload.koi;
        }
    }

    private static class ClientIdData {
        @SerializedName("data_payload")
        public ClientIdDataPayload dataPayload;

    }

    private static class ClientIdDataPayload {
        public ClientIdMeta koi;

    }

}
