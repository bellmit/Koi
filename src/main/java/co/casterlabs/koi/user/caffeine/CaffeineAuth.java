package co.casterlabs.koi.user.caffeine;

import java.util.Collections;
import java.util.Map;

import com.github.caffeineapi.requests.CaffeineSendChatMessageRequest;

import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;

public class CaffeineAuth extends com.github.caffeineapi.CaffeineAuth implements AuthProvider {

    public CaffeineAuth(String refreshToken) {
        this.loginBlocking(refreshToken);
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        return Collections.singletonMap("Authorization", "Bearer " + this.getAccessToken());
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
    public void refresh() throws Exception {
        this.login(this.getRefreshToken());
    }

    @Override
    public void sendChatMessage(User user, String message) {
        CaffeineSendChatMessageRequest request = new CaffeineSendChatMessageRequest(this);

        request.setCAID(user.getUUID());
        request.setMessage(message);

        request.sendAsync();
    }

}
