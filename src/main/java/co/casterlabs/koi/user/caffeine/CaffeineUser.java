package co.casterlabs.koi.user.caffeine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.caffeineapi.requests.CaffeineFollowersListRequest;
import co.casterlabs.caffeineapi.requests.CaffeineFollowersListRequest.CaffeineFollower;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import lombok.ToString;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class CaffeineUser extends User {
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

        CaffeineFollowersListRequest request = new CaffeineFollowersListRequest((CaffeineAuth) Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE));

        request.setCAID(this.UUID);
        request.sendAsync().thenAccept((followers) -> {
            for (CaffeineFollower follower : followers) {
                this.followers.add(follower.getCAID());
            }
        });
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
        try {
            CaffeineFollowersListRequest request = new CaffeineFollowersListRequest((CaffeineAuth) Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE));

            request.setCAID(this.UUID);

            List<CaffeineFollower> followers = request.send();

            for (CaffeineFollower follower : followers) {
                if (this.followers.add(follower.getCAID())) {
                    SerializedUser user = CaffeineUserConverter.getInstance().get(follower.getCAID());

                    this.broadcastEvent(new FollowEvent(user, this));
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void updateUser() throws IdentifierException {
        try {
            FastLogger.logStatic(LogLevel.DEBUG, "Polled %s/%s", this.UUID, this.getUsername());
            CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

            request.setCAID(this.UUID);

            try {
                this.updateUser(request.send());
            } catch (Exception e) {
                e.printStackTrace();
                throw new IdentifierException();
            }
        } catch (IdentifierException e) {
            throw e;
        } catch (Exception ignored) {}
    }

    @Override
    public void updateUser(@Nullable Object obj) {
        if (obj != null) {
            if (obj instanceof co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest.CaffeineUser) {
                CaffeineUserInfoRequest.CaffeineUser data = (CaffeineUserInfoRequest.CaffeineUser) obj;

                this.UUID = data.getCAID();
                this.setUsername(data.getUsername());
                this.imageLink = data.getImageLink();
                this.followerCount = data.getFollowersCount();
                this.displayname = ((data.getDisplayname() == null) || data.getDisplayname().isEmpty()) ? this.getUsername() : data.getDisplayname();

                this.broadcastEvent(new UserUpdateEvent(this));
            } else if (obj instanceof JsonObject) {
                JsonObject data = (JsonObject) obj;
                JsonElement nameJson = data.get("name");

                this.setUsername(data.get("username").getAsString());
                this.imageLink = CaffeineLinks.getAvatarLink(data.get("avatar_image_path").getAsString());
                this.displayname = (nameJson.isJsonNull()) ? this.getUsername() : nameJson.getAsString();
                this.followerCount = data.get("followers_count").getAsLong();
                this.UUID = data.get("caid").getAsString();

                this.broadcastEvent(new UserUpdateEvent(this));
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
