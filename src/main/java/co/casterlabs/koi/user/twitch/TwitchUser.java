package co.casterlabs.koi.user.twitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PolyFillRequirements;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.twitchapi.HttpUtil;
import co.casterlabs.twitchapi.TwitchApi;
import co.casterlabs.twitchapi.helix.HelixGetStreamsRequest;
import co.casterlabs.twitchapi.helix.HelixGetStreamsRequest.HelixStream;
import co.casterlabs.twitchapi.helix.HelixGetUserFollowsRequest;
import co.casterlabs.twitchapi.helix.HelixGetUserFollowsRequest.HelixFollower;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest.HelixUser;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest.WebhookSubscribeMode;
import lombok.Getter;
import okhttp3.Response;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchUser extends User {
    private List<HelixWebhookSubscribeRequest> webhooks = new ArrayList<>();
    private @Getter TwitchMessages messages;

    public TwitchUser(String identifier, Object data) throws IdentifierException {
        super(UserPlatform.TWITCH);

        if (data == null) {
            this.UUID = identifier; // TEMP for updateUser();

            try {
                FastLogger.logStatic(LogLevel.DEBUG, "Polled %s/%s", this.UUID, this.getUsername());
                SerializedUser user = TwitchUserConverter.getInstance().getByLogin(this.UUID);

                this.updateUser(user);
                this.updateUser(); // A bit backwards, but this is a special implementation for Twitch WebSub.
            } catch (Exception e) {
                throw e;
            }
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

        if (this.webhooks.isEmpty()) {
            TwitchAuth auth = (TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);

            HelixGetUserFollowsRequest followersRequest = new HelixGetUserFollowsRequest(this.UUID, auth);
            HelixGetStreamsRequest streamsRequest = new HelixGetStreamsRequest(auth);

            streamsRequest.addId(this.UUID);

            try {
                for (HelixFollower follower : followersRequest.send()) {
                    this.followers.add(follower.getId());
                }

                List<HelixStream> streams = streamsRequest.send();

                if (streams.isEmpty()) {
                    this.broadcastEvent(new StreamStatusEvent(false, "", this));
                } else {
                    HelixStream stream = streams.get(0);

                    this.broadcastEvent(new StreamStatusEvent(true, stream.getTitle(), this));
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }

            try {
                this.webhooks.add(TwitchWebhookEndpoint.getInstance().addFollowerHook(this.UUID, (follower) -> {
                    try {
                        HelixUser helix = follower.getAsUser(auth);

                        if (this.followers.add(helix.getId())) {
                            SerializedUser user = TwitchUserConverter.convert(helix);

                            this.broadcastEvent(new FollowEvent(user, this));
                        }
                    } catch (ApiException | IOException e) {
                        e.printStackTrace();
                    }
                }));

                this.webhooks.add(TwitchWebhookEndpoint.getInstance().addStreamHook(this.UUID, (stream) -> {
                    if (stream == null) {
                        this.broadcastEvent(new StreamStatusEvent(false, "", this));
                    } else {
                        this.broadcastEvent(new StreamStatusEvent(true, stream.getTitle(), this));
                    }
                }));

                this.webhooks.add(TwitchWebhookEndpoint.getInstance().addUserProfileHook(this.UUID, (helix) -> {
                    SerializedUser user = TwitchUserConverter.convert(helix);

                    this.updateUser(user);
                }));
            } catch (ApiException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setFollowerCount(long count) {
        this.followerCount = count;
    }

    @Override
    protected void update0() {}

    @Override
    protected void updateUser() throws IdentifierException {
        try {
            String url = String.format("https://api.twitch.tv/helix/users/follows?to_id=%s", this.UUID);
            Response response = HttpUtil.sendHttpGet(url, null, (TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));
            JsonObject json = TwitchApi.GSON.fromJson(response.body().string(), JsonObject.class);
            long newCount = json.get("total").getAsLong();

            if (this.followerCount != newCount) {
                this.followerCount = newCount;

                this.broadcastEvent(new UserUpdateEvent(this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateUser(@Nullable Object obj) {
        if ((obj != null) && (obj instanceof SerializedUser)) {
            SerializedUser serialized = (SerializedUser) obj;

            this.UUID = serialized.getUUID();
            this.setUsername(serialized.getUsername());
            this.imageLink = serialized.getImageLink();

            if (this.preferences != null) {
                this.preferences.set(PolyFillRequirements.PROFILE_PICTURE, this.imageLink);
            }

            this.broadcastEvent(new UserUpdateEvent(this));
        }
    }

    @Override
    protected void close0(JsonObject save) {
        if (this.messages != null) this.messages.close();

        for (HelixWebhookSubscribeRequest webhook : this.webhooks) {
            try {
                webhook.setAutoRefresh(false).setMode(WebhookSubscribeMode.UNSUBSCRIBE).send();
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Provider implements UserProvider {
        @Override
        public User get(String identifier, Object data) throws IdentifierException {
            return new TwitchUser(identifier, data);
        }
    }

}
