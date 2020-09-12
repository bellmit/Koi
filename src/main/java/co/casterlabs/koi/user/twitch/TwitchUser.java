package co.casterlabs.koi.user.twitch;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.koi.util.WebUtil;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchUser extends User {
    private TwitchFollowerChecker followerChecker = new TwitchFollowerChecker(this);
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
        this.followerChecker.updateFollowers();
    }

    @Override
    public void tryExternalHook() {
        if (this.messages != null) {
            this.messages.close();
        }

        this.messages = new TwitchMessages(this);
    }

    public void setFollowerCount(long count) {
        this.followerCount = count;
    }

    @Override
    protected void update0() {
        this.followerChecker.updateFollowers();

        JsonObject json = WebUtil.jsonSendHttpGet(TwitchLinks.getStreamInfo(this.UUID), Koi.getInstance().getAuthProvider(UserPlatform.TWITCH).getAuthHeaders(), JsonObject.class);
        StreamStatusEvent oldStatus = (StreamStatusEvent) this.dataEvents.getOrDefault(EventType.STREAM_STATUS, new StreamStatusEvent(false, "", this));

        if (json.get("stream").isJsonNull() && oldStatus.isLive()) {
            this.broadcastEvent(new StreamStatusEvent(false, "", this));
        } else {
            String title = json.getAsJsonObject("stream").getAsJsonObject("channel").get("status").getAsString();

            if (!title.equals(oldStatus.getTitle())) {
                this.broadcastEvent(new StreamStatusEvent(true, title, this));
            }
        }
    }

    @SneakyThrows
    @Override
    protected void updateUser() {
        try {
            FastLogger.logStatic(LogLevel.DEBUG, "Polled %s/%s", this.UUID, this.getUsername());
            SerializedUser user = TwitchUserConverter.getInstance().get(this.UUID);

            this.updateUser(user);
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Poll for Twitch user %s failed.", this.UUID);
            FastLogger.logException(e);
        }
    }

    @Override
    public void updateUser(@Nullable Object obj) {
        if ((obj != null) && (obj instanceof SerializedUser)) {
            SerializedUser serialized = (SerializedUser) obj;

            this.UUID = serialized.getUUID();
            this.setUsername(serialized.getUsername());
            this.imageLink = serialized.getImageLink();
            this.displayname = serialized.getDisplayname();
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
