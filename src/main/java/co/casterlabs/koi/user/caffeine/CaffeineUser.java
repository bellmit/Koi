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
    private @ToString.Exclude CaffeineFollowerChecker followerChecker = new CaffeineFollowerChecker(this);
    private @ToString.Exclude CaffeineMessages messageSocket;
    private @ToString.Exclude CaffeineQuery querySocket;

    private @Getter String stageId;

    private CaffeineUser(String identifier, JsonObject json) throws IdentifierException {
        super(UserPlatform.CAFFEINE);

        this.UUID = identifier; // TEMP for updateUser();

        if (json != null) {
            JsonObject user = json.get("user").getAsJsonObject();
            JsonElement nameJson = user.get("name");

            this.setUsername(user.get("username").getAsString());
            this.imageLink = CaffeineLinks.getAvatarLink(user.get("avatar_image_path").getAsString());
            this.displayname = (nameJson.isJsonNull()) ? this.getUsername() : nameJson.getAsString();
            this.stageId = user.get("stage_id").getAsString();
            this.followerCount = user.get("followers_count").getAsLong();
            this.followingCount = user.get("following_count").getAsLong();
            this.UUID = user.get("caid").getAsString();
        } else {
            this.updateUser();
        }

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
        this.followerChecker.updateFollowers();
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
            this.followerCount = user.get("followers_count").getAsLong();
            this.followingCount = user.get("following_count").getAsLong();
            this.UUID = user.get("caid").getAsString();
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception e) {
            Koi.getInstance().getLogger().severe(String.format("Poll for Caffeine user \"%s\" failed.", this.UUID));
            Koi.getInstance().getLogger().exception(e);
        }
    }

    public static class Unsafe {
        public static CaffeineUser get(String identifier, JsonObject userdata) throws IdentifierException {
            return new CaffeineUser(identifier, userdata);
        }
    }

}
