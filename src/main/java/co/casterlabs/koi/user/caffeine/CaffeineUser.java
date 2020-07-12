package co.casterlabs.koi.user.caffeine;

import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.IdentifierException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;

public class CaffeineUser extends User {
    private static final String[] COLORS = new String[] { "#E6194B", "#3CB44B", "#FFE119", "#4363D8", "#F58231", "#911EB4", "#46F0F0", "#F032E6", "#BCF60C", "#FABEBE", "#008080", "#E6BEFF", "#9A6324", "#800000", "#AAFFC3", "#808000", "#000075"
    };

    private @ToString.Exclude CaffeineFollowerChecker followerChecker = new CaffeineFollowerChecker(this);
    private @ToString.Exclude CaffeineMessages messageSocket;
    private @ToString.Exclude CaffeineQuery querySocket;

    private @Getter String stageId;

    private CaffeineUser(String identifier, Object data) throws IdentifierException {
        super(UserPlatform.CAFFEINE);

        if (data == null) {
            this.UUID = identifier; // TEMP for updateUser();

            this.updateUser();
        } else {
            this.updateUser(data);
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
            Koi.getInstance().getLogger().debug("Polled %s/%s", this.UUID, this.getUsername());
            JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getUsersLink(this.UUID), null, JsonObject.class);

            if (json.has("errors")) {
                throw new IdentifierException();
            }

            this.updateUser(json.get("user"));
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception e) {
            Koi.getInstance().getLogger().severe("Poll for Caffeine user %s failed.", this.UUID);
            Koi.getInstance().getLogger().exception(e);
        }
    }

    @Override
    public void updateUser(@Nullable Object obj) {
        if ((obj != null) && (obj instanceof JsonObject)) {
            JsonObject data = (JsonObject) obj;
            JsonElement nameJson = data.get("name");

            this.setUsername(data.get("username").getAsString());
            this.imageLink = CaffeineLinks.getAvatarLink(data.get("avatar_image_path").getAsString());
            this.displayname = (nameJson.isJsonNull()) ? this.getUsername() : nameJson.getAsString();
            this.stageId = data.get("stage_id").getAsString();
            this.followerCount = data.get("followers_count").getAsLong();
            this.followingCount = data.get("following_count").getAsLong();
            this.UUID = data.get("caid").getAsString();

            if (this.color == null) {
                this.color = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
            }
        }
    }

    public static class Provider implements UserProvider {
        @Override
        public User get(String identifier, Object data) throws IdentifierException {
            return new CaffeineUser(identifier, data);
        }
    }

}
