package co.casterlabs.koi.user.twitch;

import java.io.Closeable;
import java.io.IOException;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.Client;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest.HelixUser;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest.WebhookSubscribeMode;
import lombok.NonNull;

public class TwitchWebhookAdapter {
    private static final Closeable DEAD_CLOSEABLE = new Closeable() {
        @Override
        public void close() throws IOException {}
    };

    public static Closeable hookFollowers(@NonNull ConnectionHolder holder) {
        try {
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addFollowerHook(holder.getProfile().getUUID(), (follower) -> {
                try {
                    TwitchHelixAuth auth = (TwitchHelixAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);

                    HelixUser helix = follower.getAsUser(auth);
                    User user = TwitchUserConverter.transform(helix);

                    holder.broadcastEvent(new FollowEvent(user, holder.getProfile()));
                } catch (ApiException | IOException e) {
                    e.printStackTrace();
                }
            });

            return (new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        request.setAutoRefresh(false);
                        request.setMode(WebhookSubscribeMode.UNSUBSCRIBE);

                        request.send();
                    } catch (ApiException e) {
                        throw new IOException(e);
                    }
                }
            });
        } catch (ApiException | IOException e) {
            e.printStackTrace();
        }

        return DEAD_CLOSEABLE;
    }

    public static Closeable hookStream(@NonNull ConnectionHolder holder) {
        try {
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addStreamHook(holder.getProfile().getUUID(), (stream) -> {
                if (stream == null) {
                    holder.broadcastEvent(new StreamStatusEvent(false, "", holder.getProfile()));
                } else {
                    holder.broadcastEvent(new StreamStatusEvent(true, stream.getTitle(), holder.getProfile()));
                }
            });

            return (new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        request.setAutoRefresh(false);
                        request.setMode(WebhookSubscribeMode.UNSUBSCRIBE);

                        request.send();
                    } catch (ApiException e) {
                        throw new IOException(e);
                    }
                }
            });
        } catch (ApiException | IOException e) {
            e.printStackTrace();
        }

        return DEAD_CLOSEABLE;
    }

    public static Closeable hookProfile(Client user, @NonNull ConnectionHolder holder) {
        try {
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addUserProfileHook(holder.getProfile().getUUID(), (helix) -> {
                User profile = TwitchUserConverter.transform(helix);

                user.updateProfileSafe(profile);
            });

            return (new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        request.setAutoRefresh(false);
                        request.setMode(WebhookSubscribeMode.UNSUBSCRIBE);

                        request.send();
                    } catch (ApiException e) {
                        throw new IOException(e);
                    }
                }
            });
        } catch (ApiException | IOException e) {
            e.printStackTrace();
        }

        return DEAD_CLOSEABLE;
    }

}
