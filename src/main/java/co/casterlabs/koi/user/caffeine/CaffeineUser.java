package co.casterlabs.koi.user.caffeine;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.koi.util.WebUtil;
import lombok.SneakyThrows;
import lombok.ToString;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class CaffeineUser extends User {
    private @ToString.Exclude CaffeineFollowerChecker followerChecker = new CaffeineFollowerChecker(this);
    private @ToString.Exclude CaffeineMessages messageSocket;
    private @ToString.Exclude CaffeineQuery querySocket;

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

    @Override
    protected void close0(JsonObject save) {
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
            FastLogger.logStatic(LogLevel.DEBUG, "Polled %s/%s", this.UUID, this.getUsername());
            JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getUsersLink(this.UUID), null, JsonObject.class);

            if (json.has("errors")) {
                throw new IdentifierException();
            }

            this.updateUser(json.get("user"));
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Poll for Caffeine user %s failed.", this.UUID);
            FastLogger.logException(e);
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
            this.followerCount = data.get("followers_count").getAsLong();
            this.UUID = data.get("caid").getAsString();
        }
    }

    public static class Provider implements UserProvider {
        @Override
        public User get(String identifier, Object data) throws IdentifierException {
            return new CaffeineUser(identifier, data);
        }
    }

}
