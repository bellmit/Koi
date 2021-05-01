package co.casterlabs.koi;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.ClientAuthProvider;

public interface PlatformAuthorizer {

    public ClientAuthProvider authorize(String token, Natsukashii.AuthData data) throws ApiAuthException, ApiException;

}
