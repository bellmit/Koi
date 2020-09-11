package co.casterlabs.koi.user.twitch;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.koi.util.WebUtil;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchUser extends User {
    private TwitchMessages messages;

    public TwitchUser(String identifier, Object data) {
        super(UserPlatform.TWITCH);

        if (data == null) {
            this.UUID = identifier; // TEMP for updateUser();

            this.updateUser();
        } else {
            this.updateUser(data);
        }

        this.load();
    }

    @Override
    public void tryExternalHook() {
        if (this.messages != null) {
            this.messages.close();
        }

        this.messages = new TwitchMessages(this);
        // TODO stream query https://dev.twitch.tv/docs/v5/reference/streams/#get-stream-by-user
        // TODO followers https://dev.twitch.tv/docs/v5/reference/channels/#get-channel-followers
    }

    @Override
    protected void update0() {}

    @SneakyThrows
    @Override
    protected void updateUser() {
        try {
            FastLogger.logStatic(LogLevel.DEBUG, "Polled %s/%s", this.UUID, this.getUsername());
            JsonObject data = null;

            if (this.getUsername() == null) {
                JsonObject json = WebUtil.jsonSendHttpGet(TwitchLinks.getUserByLoginLink(this.UUID), null, JsonObject.class);

                if (json.get("_total").getAsInt() != 0) {
                    data = json.getAsJsonArray("users").get(0).getAsJsonObject();
                }
            }

            if (data == null) {
                data = WebUtil.jsonSendHttpGet(TwitchLinks.getUserByIdLink(this.UUID), null, JsonObject.class);

                if (data.has("error")) {
                    throw new IdentifierException();
                }
            }

            this.updateUser(data);
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Poll for Twitch user %s failed.", this.UUID);
            FastLogger.logException(e);
        }
    }

    @Override
    public void updateUser(@Nullable Object obj) {
        if ((obj != null) && (obj instanceof JsonObject)) {
            JsonObject json = (JsonObject) obj;

            this.displayname = json.get("display_name").getAsString();
            this.setUsername(json.get("name").getAsString());
            this.UUID = json.get("_id").getAsString();
            this.imageLink = json.get("logo").getAsString();
        }
    }

    @Override
    protected void close0(JsonObject save) {
        if (this.messages != null) this.messages.close();
    }

    public static class Provider implements UserProvider {

        @Override
        public User get(String identifier, Object data) throws IdentifierException {
            return new TwitchUser(identifier, data);
        }

    }

}
