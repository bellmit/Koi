package co.casterlabs.koi;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;

public interface PlatformIntegration {

    public ClientAuthProvider getAppAuth();

    public UserConverter<?> getUserConverter();

    public UserProvider getUserProvider();

    public PlatformAuthorizer getPlatformAuthorizer();

    public UserPlatform getPlatform();

}
