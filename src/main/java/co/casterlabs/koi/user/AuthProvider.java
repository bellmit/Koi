package co.casterlabs.koi.user;

public interface AuthProvider {

    public UserPlatform getPlatform();

    public boolean isLoggedIn();

    public void refresh() throws Exception;

    public void sendChatMessage(User user, String message);

}
