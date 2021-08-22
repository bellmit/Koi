package co.casterlabs.koi;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.PlatformProvider;

public interface PlatformIntegration {

    public ClientAuthProvider getAppAuth();

    public UserConverter<?> getUserConverter();

    public PlatformProvider getUserProvider();

    public PlatformAuthorizer getPlatformAuthorizer();

    public UserPlatform getPlatform();

}
