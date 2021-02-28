package co.casterlabs.koi.user.twitch;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import co.casterlabs.twitchapi.helix.types.HelixStream;
import co.casterlabs.twitchapi.helix.types.HelixUser;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest.WebhookSubscribeMode;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchWebhookAdapter {
    public static final Closeable DEAD_CLOSEABLE = new Closeable() {
        @Override
        public void close() throws IOException {}
    };

    public static Closeable hookFollowers(@NonNull ConnectionHolder holder) {
        try {
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addFollowerHook(holder.getSimpleProfile().getUUID(), (follower) -> {
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
                        FastLogger.logStatic(LogLevel.DEBUG, "Closed follower webhook for %s", holder.getSimpleProfile());
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
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addStreamHook(holder.getSimpleProfile().getUUID(), new Consumer<HelixStream>() {
                private Instant streamStartedAt;

                @Override
                public void accept(HelixStream stream) {
                    StreamStatusEvent e;

                    if (stream == null) {
                        this.streamStartedAt = null;

                        e = new StreamStatusEvent(false, "", holder.getProfile(), this.streamStartedAt);
                    } else {
                        if (this.streamStartedAt == null) {
                            this.streamStartedAt = Instant.now();
                        }

                        e = new StreamStatusEvent(true, stream.getTitle(), holder.getProfile(), null);
                    }

                    holder.broadcastEvent(e);
                    holder.setHeldEvent(e);
                }

            });

            return (new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        request.setAutoRefresh(false);
                        request.setMode(WebhookSubscribeMode.UNSUBSCRIBE);
                        request.send();
                        FastLogger.logStatic(LogLevel.DEBUG, "Closed stream webhook for %s", holder.getSimpleProfile());
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
