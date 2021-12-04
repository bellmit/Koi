package co.casterlabs.koi.integration.twitch.connections;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.events.ChannelChangeTitleEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.twitchapi.helix.requests.HelixGetStreamsRequest;
import co.casterlabs.twitchapi.helix.types.HelixStream;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Twitch4JAdapter {
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

    public static Connection hook(@NonNull ConnectionHolder holder, boolean onlyStream) {
        // FOLLOWERS & STREAM
        // We only hold the stream status event, btw.
        TwitchClient twitchClient = TwitchClientBuilder.builder()
            .withEnableHelix(true)
            .build();

        Consumer<StreamStatusEvent> streamStatusHandler = new Consumer<StreamStatusEvent>() {
            private Instant streamStartedAt;

            @Override
            public void accept(@Nullable StreamStatusEvent e) {
                // If e is null then the stream is offline and we need to send an event for
                // that.
                // Otherwise we check to see if it's a fresh notification and set the time.
                if (e == null) {
                    this.streamStartedAt = null;

                    e = new StreamStatusEvent(false, "", holder.getProfile(), null);
                } else {
                    if (this.streamStartedAt == null) {
                        this.streamStartedAt = Instant.now();
                    }

                    // Make sure it has the correct time.
                    e.setStartTime(this.streamStartedAt);
                }

                holder.broadcastEvent(e);
                holder.setHeldEvent(e);
            }
        };

        // Register channel.
        twitchClient.getClientHelper().enableStreamEventListener(holder.getActiveProfile().getId(), holder.getActiveProfile().getUsername());

        // Disable follower notifications for the discord/guilded bot.
        if (!onlyStream) {
            twitchClient.getClientHelper().enableFollowEventListener(holder.getActiveProfile().getId(), holder.getActiveProfile().getUsername());
        }

        // Handlers.
        twitchClient.getEventManager().onEvent(ChannelGoLiveEvent.class, event -> {
            streamStatusHandler.accept(new StreamStatusEvent(true, event.getStream().getTitle(), holder.getActiveProfile(), null));
        });

        twitchClient.getEventManager().onEvent(ChannelChangeTitleEvent.class, event -> {
            streamStatusHandler.accept(new StreamStatusEvent(true, event.getStream().getTitle(), holder.getActiveProfile(), null));
        });

        twitchClient.getEventManager().onEvent(ChannelGoOfflineEvent.class, event -> {
            streamStatusHandler.accept(null);
        });

        twitchClient.getEventManager().onEvent(com.github.twitch4j.chat.events.channel.FollowEvent.class, event -> {
            try {
                String userId = event.getUser().getId();
                User follower = TwitchUserConverter.getInstance().getByID(userId);

                holder.broadcastEvent(new FollowEvent(follower, holder.getActiveProfile()));
            } catch (IdentifierException e) {
                e.printStackTrace();
            }
        });

        // The koi interaction
        return (new Connection() {
            private boolean isFirstInit = false;

            @Override
            public void close() throws IOException {
                twitchClient.close();
                FastLogger.logStatic(LogLevel.DEBUG, "Closed twitch4j for %s", holder.getSimpleProfile());
            }

            @Override
            public void open() throws IOException {
                if (!this.isFirstInit) {
                    this.isFirstInit = true;

                    try {
                        List<HelixStream> streamsResult = new HelixGetStreamsRequest(TwitchIntegration.getInstance().getAppAuth())
                            .addId(holder.getSimpleProfile().getChannelId())
                            .send();

                        // Empty means either the stream is not live or doesn't exist.
                        // Since we check for the validity of accounts before we call
                        // this method it's safe to assume they're just offline.
                        if (streamsResult.isEmpty()) {
                            // NULL is sent by the webhook api to indicate a stream is
                            // offline so we mimic that.
                            streamStatusHandler.accept(null);
                        } else {
                            holder.broadcastEvent(new StreamStatusEvent(true, streamsResult.get(0).getTitle(), holder.getActiveProfile(), null));
                        }
                    } catch (ApiException e) {
                        throw new IOException(e);
                    }
                }
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        });
    }

}
