package co.casterlabs.koi.user.caffeine;

import co.casterlabs.caffeineapi.requests.CaffeineSendChatMessageRequest;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;

public class CaffeineAuth extends co.casterlabs.caffeineapi.CaffeineAuth implements AuthProvider {

    public CaffeineAuth(String refreshToken) {
        this.loginBlocking(refreshToken);
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
