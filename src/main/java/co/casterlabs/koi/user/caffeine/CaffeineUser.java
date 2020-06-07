package co.casterlabs.koi.user.caffeine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.IdentifierException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;

public class CaffeineUser extends User {
    private @ToString.Exclude CaffeineUserChecker caffeineChecker = new CaffeineUserChecker(this);
    private @ToString.Exclude CaffeineMessages messageSocket;
    private @ToString.Exclude CaffeineQuery querySocket;

    private @Getter String stageId;

    private CaffeineUser(String identifier) throws IdentifierException {
        super(UserPlatform.CAFFEINE);

        this.UUID = identifier; // TEMP for update0();

        this.updateUser();
        this.load();
    }

    public String getCAID() {
        return this.UUID;
    }

    @Override
    protected void close0() {
        if (this.messageSocket != null) this.messageSocket.close();
        if (this.querySocket != null) this.querySocket.close();
    }

    @Override
    public void tryExternalHook() {
        if (this.messageSocket == null) {
            this.messageSocket = new CaffeineMessages(this);
        } else if (!this.messageSocket.isOpen()) {
            this.messageSocket.reconnect();
        }

        if (this.querySocket == null) {
            this.querySocket = new CaffeineQuery(this);
        } else if (!this.querySocket.isOpen()) {
            this.querySocket.reconnect();
        }

        this.wake();
    }

    @Override
    protected void update0() {
        this.caffeineChecker.updateFollowers();
    }

    @SneakyThrows
    @Override
    protected void updateUser() {
        try {
            JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getUsersLink(this.UUID), null, JsonObject.class);

            if (json.has("errors")) throw new IdentifierException();

            JsonObject user = json.get("user").getAsJsonObject();
            JsonElement nameJson = user.get("name");

            this.setUsername(user.get("username").getAsString());
            this.imageLink = CaffeineLinks.getAvatarLink(user.get("avatar_image_path").getAsString());
            this.displayname = (nameJson.isJsonNull()) ? this.getUsername() : nameJson.getAsString();
            this.stageId = user.get("stage_id").getAsString();
            this.UUID = user.get("caid").getAsString();

            this.wake();
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception e) {
            Koi.getInstance().getLogger().warn(String.format("Poll for Caffeine user \"%s\" failed.", this.UUID));
        }
    }

    public static class Unsafe {
        public static CaffeineUser get(String identifier) throws IdentifierException {
            return new CaffeineUser(identifier);
        }
    }

}
