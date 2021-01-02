package co.casterlabs.koi.user;

import com.google.gson.JsonObject;

public interface KoiAuthProvider {

    public UserPlatform getPlatform();

    public boolean isValid();

    public void refresh() throws Exception;

    public JsonObject getCredentials();

}
