package co.casterlabs.koi.user.caffeine;

import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.caffeineapi.requests.CaffeineSendChatMessageRequest;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;

public class CaffeineAuth extends co.casterlabs.caffeineapi.CaffeineAuth implements AuthProvider {
    private static @Getter String anonymousCredential;

    static {
        new RepeatingThread("Caffeine Anonymous Credential Refresh", TimeUnit.MINUTES.toMillis(5), () -> {
            JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getAnonymousCredentialLink(), null, JsonObject.class);

            anonymousCredential = json.get("credential").getAsString();
        }).start();
    }

    public CaffeineAuth(String refreshToken) throws ApiAuthException {
        this.login(refreshToken);
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.CAFFEINE;
    }

    @Override
    public boolean isLoggedIn() {
        return this.isValid();
    }

    @Override
    public void sendChatMessage(User user, String message) {
        Koi.getMiscThreadPool().submit(() -> {
            try {
                CaffeineSendChatMessageRequest request = new CaffeineSendChatMessageRequest(this);

                request.setCAID(user.getUUID());
                request.setMessage(message);

                request.send();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
