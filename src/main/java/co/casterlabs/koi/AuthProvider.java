package co.casterlabs.koi;

import java.util.Map;

import co.casterlabs.koi.user.UserPlatform;

public interface AuthProvider {
    public Map<String, String> getAuthHeaders();

    public UserPlatform getPlatform();

    public boolean isLoggedIn();

}
