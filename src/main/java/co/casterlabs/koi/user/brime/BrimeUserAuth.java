package co.casterlabs.koi.user.brime;

import java.util.Base64;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;

@Getter
public class BrimeUserAuth implements ClientAuthProvider {
    private String token;
    private String username;
    private String role;

    public BrimeUserAuth(String jwt) {
        String[] split = jwt.split("\\.");

        String payload = new String(Base64.getDecoder().decode(split[1]));

        JsonObject data = Koi.GSON.fromJson(payload, JsonObject.class);

        this.token = jwt;
        this.username = data.get("username").getAsString();
        this.role = data.get("role").getAsString();
    }

    public String getUUID() {
        return "BRIME_BETA_LEGACY." + this.username;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.BRIME;
    }

    @Override
    public boolean isValid() {
        return true; // No way to test PEPEGACLAP
    }

    @Override
    public void refresh() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject getCredentials() {
        throw new UnsupportedOperationException();
    }

}
