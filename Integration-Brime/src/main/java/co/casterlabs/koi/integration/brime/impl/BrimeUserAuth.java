package co.casterlabs.koi.integration.brime.impl;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.brimeapijava.requests.BrimeGetChannelRequest;
import co.casterlabs.brimeapijava.types.BrimeChannel;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class BrimeUserAuth extends co.casterlabs.brimeapijava.BrimeUserAuth implements ClientAuthProvider {
    private String channelId;
    private String channelName;
    private SimpleProfile simpleProfile;

    public BrimeUserAuth(@NonNull String clientId, @NonNull String refreshToken) throws ApiAuthException, ApiException {
        super(clientId, refreshToken);

        BrimeChannel channel = new BrimeGetChannelRequest(this).setChannel("me").send();

        this.channelId = channel.getChannelId();
        this.channelName = channel.getChannelName();
        this.simpleProfile = new SimpleProfile(
            channel.getOwners().get(0), // TODO
            this.channelId,
            UserPlatform.BRIME
        );
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.BRIME;
    }

    @Override
    public boolean isValid() {
        try {
            this.refresh();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public JsonObject getCredentials() {
        JsonObject payload = new JsonObject();

        payload.addProperty("authorization", "Bearer " + this.accessToken);
        payload.addProperty("client_id", this.clientId);

        return payload;
    }

    @Override
    public SimpleProfile getSimpleProfile() {
        return this.simpleProfile;
    }

}
