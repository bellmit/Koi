package co.casterlabs.koi.integration.brime.impl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.brimeapijava.realtime.BrimeRealtime;
import co.casterlabs.brimeapijava.requests.BrimeDeleteChatMessageRequest;
import co.casterlabs.brimeapijava.requests.BrimeGetChannelRequest;
import co.casterlabs.brimeapijava.requests.BrimeGetStreamRequest;
import co.casterlabs.brimeapijava.requests.BrimeGetUserRequest;
import co.casterlabs.brimeapijava.requests.BrimeSendChatMessageRequest;
import co.casterlabs.brimeapijava.types.BrimeChannel;
import co.casterlabs.brimeapijava.types.BrimeStream;
import co.casterlabs.brimeapijava.types.BrimeUser;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionCache;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.integration.brime.BrimeIntegration;
import co.casterlabs.koi.integration.brime.connections.BrimeRealtimeAdapter;
import co.casterlabs.koi.integration.brime.data.BrimeUserConverter;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.RepeatingThread;
import lombok.NonNull;
import lombok.SneakyThrows;

public class BrimeProvider implements PlatformProvider {

    private static ConnectionCache realtimeConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SuppressWarnings("deprecation")
        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            BrimeRealtime realtime = new BrimeRealtime(BrimeIntegration.getInstance().getAblySecret(), holder.getSimpleProfile().getChannelId());
            BrimeRealtimeAdapter adapter = new BrimeRealtimeAdapter(holder, realtime);

            realtime.setListener(adapter);

            return adapter;
        }

    };

    private static ConnectionCache streamPollerCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            String channelId = holder.getSimpleProfile().getChannelId();

            return new RepeatingThread("Brime stream poller " + channelId, TimeUnit.MINUTES.toMillis(1), new Runnable() {
                private Instant streamStartedAt;

                private String title = "";

                @Override
                public void run() {
                    try {
                        BrimeStream stream = new BrimeGetStreamRequest(BrimeIntegration.getInstance().getAppAuth())
                            .setChannel(channelId)
                            .send();

                        boolean isLive = stream.isLive();

                        this.title = stream.getTitle();

                        if (isLive) {
                            if (this.streamStartedAt == null) {
                                this.streamStartedAt = Instant.now();
                            }
                        } else {
                            this.streamStartedAt = null;
                        }

                        StreamStatusEvent e = new StreamStatusEvent(isLive, this.title, holder.getProfile(), this.streamStartedAt);

                        holder.broadcastEvent(e);
                        holder.setHeldEvent(e);
                    } catch (ApiException e) {}
                }
            });
        }

    };

    @Override
    public void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            BrimeUserAuth brimeAuth = (BrimeUserAuth) auth;

            User asUser = getProfile(brimeAuth);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(getProfileUpdater(client, brimeAuth));

            client.addConnection(realtimeConnCache.get(asUser.getChannelId(), null, asUser.getSimpleProfile()));
            client.addConnection(streamPollerCache.get(asUser.getChannelId(), null, asUser.getSimpleProfile()));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        User asUser = BrimeUserConverter.getInstance().get(username);

        if (asUser == null) {
            throw new IdentifierException();
        } else {
            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(streamPollerCache.get(asUser.getChannelId(), null, asUser.getSimpleProfile()));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        }
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) throws ApiAuthException {
        try {
            new BrimeSendChatMessageRequest((BrimeUserAuth) auth)
                .setChannelId(client.getSimpleProfile().getChannelId())
                .setColor("#ea4c4c")
                .setMessage(message)
                .send();
        } catch (ApiAuthException e) {
            throw e;
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteMessage(@NonNull Client client, @NonNull String messageId, @NonNull ClientAuthProvider auth) {
        try {
            new BrimeDeleteChatMessageRequest((BrimeUserAuth) auth)
                .setChannelId(client.getSimpleProfile().getChannelId())
                .setMessageId(messageId)
                .send();
        } catch (ApiException ignored) {}
    }

    private static ConnectionHolder getProfileUpdater(Client client, BrimeUserAuth brimeAuth) {
        ConnectionHolder holder = new ConnectionHolder(brimeAuth.getChannelId(), brimeAuth.getSimpleProfile());

        holder.setConn(new RepeatingThread("Brime profile updater " + brimeAuth.getChannelId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                User asUser = getProfile(brimeAuth);

                brimeAuth.refresh();

                client.broadcastEvent(new UserUpdateEvent(asUser));
            } catch (ApiException e) {
                client.notifyCredentialExpired();
            }
        }));

        return holder;
    }

    private static User getProfile(BrimeUserAuth brimeAuth) throws ApiAuthException, ApiException {
        BrimeChannel channel = new BrimeGetChannelRequest(brimeAuth)
            .setChannel("me")
            .send();

        BrimeUser user = new BrimeGetUserRequest(brimeAuth)
            .setName("me")
            .send();

        User asUser = BrimeUserConverter.getInstance().transform(user);

        asUser.setChannelId(channel.getChannelId());

        asUser.setFollowersCount(channel.getFollowerCount());
        asUser.setSubCount(channel.getSubscriberCount());

        return asUser;
    }

}
