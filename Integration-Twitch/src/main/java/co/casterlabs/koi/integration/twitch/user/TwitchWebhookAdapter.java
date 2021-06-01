package co.casterlabs.koi.integration.twitch.user;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.Connection;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.integration.twitch.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.User;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import co.casterlabs.twitchapi.helix.types.HelixStream;
import co.casterlabs.twitchapi.helix.types.HelixUser;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest.WebhookSubscribeMode;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchWebhookAdapter {
    public static final Connection DEAD_CONNECTION = new Connection() {
        @Override
        public void close() throws IOException {}

        @Override
        public void open() throws IOException {}

        @Override
        public boolean isOpen() {
            return true;
        }
    };

    public static Connection hookFollowers(@NonNull ConnectionHolder holder) {
        try {
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addFollowerHook(holder.getSimpleProfile().getChannelId(), (follower) -> {
                try {
                    TwitchHelixAuth auth = TwitchIntegration.getInstance().getAppAuth();

                    HelixUser helix = follower.getAsUser(auth);
                    User user = TwitchUserConverter.transform(helix);

                    holder.broadcastEvent(new FollowEvent(user, holder.getProfile()));
                } catch (ApiException | IOException e) {
                    e.printStackTrace();
                }
            });

            return (new Connection() {
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

                @Override
                public void open() throws IOException {}

                @Override
                public boolean isOpen() {
                    return true;
                }
            });
        } catch (ApiException | IOException e) {
            e.printStackTrace();
        }

        return DEAD_CONNECTION;
    }

    public static Connection hookStream(@NonNull ConnectionHolder holder) {
        try {
            HelixWebhookSubscribeRequest request = TwitchWebhookEndpoint.getInstance().addStreamHook(holder.getSimpleProfile().getChannelId(), new Consumer<HelixStream>() {
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

            return (new Connection() {
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

                @Override
                public void open() throws IOException {}

                @Override
                public boolean isOpen() {
                    return true;
                }
            });
        } catch (ApiException | IOException e) {
            e.printStackTrace();
        }

        return DEAD_CONNECTION;
    }

}
