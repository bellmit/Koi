package co.casterlabs.koi.client;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.UserPlatform;

public interface ClientAuthProvider {

    public UserPlatform getPlatform();

    public boolean isValid();

    public void refresh() throws Exception;

    public JsonObject getCredentials();

    public SimpleProfile getSimpleProfile();

}
