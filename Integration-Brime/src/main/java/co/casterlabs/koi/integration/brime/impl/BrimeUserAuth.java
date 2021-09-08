package co.casterlabs.koi.integration.brime.impl;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.brimeapijava.requests.BrimeGetAccountRequest;
import co.casterlabs.brimeapijava.requests.BrimeGetChannelRequest;
import co.casterlabs.brimeapijava.types.BrimeAccount;
import co.casterlabs.brimeapijava.types.BrimeChannel;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class BrimeUserAuth extends co.casterlabs.brimeapijava.BrimeAuth implements ClientAuthProvider {
    private SimpleProfile simpleProfile;
    private String clientId;

    public BrimeUserAuth(@NonNull String refreshToken, @NonNull String clientId, @NonNull String clientSecret) throws ApiAuthException, ApiException {
        super(refreshToken, clientId, clientSecret);

        BrimeAccount account = new BrimeGetAccountRequest(this)
            .send();

        BrimeChannel channel = new BrimeGetChannelRequest()
            .queryBySlug(account.getUsername())
            .send();

        this.clientId = clientId;
        this.simpleProfile = new SimpleProfile(
            account.getXid(),
            channel.getChannel().getXid(),
            UserPlatform.BRIME
        );
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.BRIME;
    }

    @Override
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("authorization", "Bearer " + this.getAccessToken());
        payload.addProperty("client_id", this.clientId);

        return payload;
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        return this.simpleProfile;
    }

}
