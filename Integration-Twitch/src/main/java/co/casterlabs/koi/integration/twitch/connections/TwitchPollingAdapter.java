package co.casterlabs.koi.integration.twitch.connections;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.twitchapi.helix.requests.HelixGetStreamsRequest;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserFollowersRequest;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserFollowersRequest.HelixFollowersResult;
import co.casterlabs.twitchapi.helix.types.HelixFollower;
import co.casterlabs.twitchapi.helix.types.HelixStream;
import lombok.NonNull;

public class TwitchPollingAdapter {

    public static Connection hookStream(@NonNull ConnectionHolder holder) {
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

        return new RepeatingThread(
            "Twitch Stream Poller - " + holder.getSimpleProfile().getChannelId(),
            TimeUnit.SECONDS.toMillis(25),
            () -> {
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
                        streamStatusHandler.accept(new StreamStatusEvent(true, streamsResult.get(0).getTitle(), holder.getActiveProfile(), null));
                    }
                } catch (ApiException ignored) {}
            }
        );
    }

    public static Connection hookFollowers(@NonNull ConnectionHolder holder) {
        Consumer<HelixFollower> followHandler = (follower) -> {
            try {
                User asUser = TwitchUserConverter.getInstance().getByID(follower.getId());

                holder.broadcastEvent(new FollowEvent(asUser, holder.getActiveProfile()));
            } catch (IdentifierException e) {}
        };

        return new RepeatingThread(
            "Twitch Stream Poller - " + holder.getSimpleProfile().getChannelId(),
            TimeUnit.SECONDS.toMillis(15),
            new Runnable() {
                private Instant latestFollowCheck = Instant.now();

                @Override
                public void run() {
                    try {
                        HelixGetUserFollowersRequest request = new HelixGetUserFollowersRequest(
                            holder.getSimpleProfile().getChannelId(),
                            TwitchIntegration.getInstance().getAppAuth()
                        );

                        request.setFirst(50);
                        HelixFollowersResult result = request.send();

                        for (HelixFollower follower : result.getFollowers()) {
                            Instant followedAt = follower.getFollowedAt();

                            if (followedAt.isAfter(this.latestFollowCheck)) {
                                followHandler.accept(follower);
                            }
                        }

                        this.latestFollowCheck = Instant.now();
                    } catch (ApiException ignored) {}
                }

            }
        );
    }

}
