package co.casterlabs.koi.user;

public interface KoiAuthProvider {

    public UserPlatform getPlatform();

    public boolean isLoggedIn();

    public void refresh() throws Exception;

}
