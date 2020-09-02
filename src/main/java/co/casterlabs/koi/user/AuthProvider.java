package co.casterlabs.koi.user;

import java.util.Map;

public interface AuthProvider {
    public Map<String, String> getAuthHeaders();

    public UserPlatform getPlatform();

    public boolean isLoggedIn();

    public void refresh() throws Exception;

}
