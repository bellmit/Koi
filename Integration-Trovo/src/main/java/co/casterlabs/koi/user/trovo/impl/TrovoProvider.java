package co.casterlabs.koi.user.trovo.impl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionCache;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.PlatformMessageEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.trovo.TrovoIntegration;
import co.casterlabs.koi.user.trovo.connections.TrovoMessages;
import co.casterlabs.koi.user.trovo.data.TrovoUserConverter;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.trovoapi.requests.TrovoDeleteChatMessageRequest;
import co.casterlabs.trovoapi.requests.TrovoGetChannelInfoRequest;
import co.casterlabs.trovoapi.requests.TrovoGetSelfInfoRequest;
import co.casterlabs.trovoapi.requests.TrovoSendChatCommandRequest;
import co.casterlabs.trovoapi.requests.TrovoSendChatCommandRequest.SendChatCommandResult;
import co.casterlabs.trovoapi.requests.TrovoSendChatMessageRequest;
import co.casterlabs.trovoapi.requests.data.TrovoChannelInfo;
import co.casterlabs.trovoapi.requests.data.TrovoSelfInfo;
import lombok.NonNull;
import lombok.SneakyThrows;

public class TrovoProvider implements PlatformProvider {

    private static ConnectionCache messagesConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            return new TrovoMessages(holder, (TrovoUserAuth) auth);
        }

    };

    private static ConnectionCache streamPollerCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            String channelId = holder.getSimpleProfile().getChannelId();
            return new RepeatingThread("Trovo stream status poller " + channelId, TimeUnit.MINUTES.toMillis(1), new Runnable() {
                private Instant streamStartedAt;

                @Override
                public void run() {
                    try {
                        TrovoGetChannelInfoRequest request = new TrovoGetChannelInfoRequest(TrovoIntegration.getInstance().getAppAuth(), channelId);

                        TrovoChannelInfo info = request.send();

                        if (info.isLive()) {
                            if (this.streamStartedAt == null) {
                                this.streamStartedAt = Instant.now();
                            }
                        } else {
                            this.streamStartedAt = null;
                        }

                        StreamStatusEvent e = new StreamStatusEvent(info.isLive(), info.getStreamTitle(), holder.getProfile(), this.streamStartedAt);

                        holder.setHeldEvent(e);
                        holder.broadcastEvent(e);
                    } catch (ApiAuthException e) {} catch (Exception ignored) {}
                }

            });
        }

    };

    @Override
    public void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            TrovoUserAuth trovoAuth = (TrovoUserAuth) auth;

            User asUser = getProfile(trovoAuth);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(getProfileUpdater(client, trovoAuth));

            client.addConnection(streamPollerCache.get(asUser.getChannelId(), trovoAuth, asUser.getSimpleProfile()));
            client.addConnection(messagesConnCache.get(asUser.getChannelId(), trovoAuth, asUser.getSimpleProfile()));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiAuthException e) {
            throw new IdentifierException();
        } catch (ApiException e) {
            e.printStackTrace();
            throw new IdentifierException();
        }
    }

    @Override
    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        User asUser = TrovoUserConverter.getInstance().getByNickname(username);

        client.setProfile(asUser);
        client.setSimpleProfile(asUser.getSimpleProfile());

        client.addConnection(streamPollerCache.get(asUser.getChannelId(), null, asUser.getSimpleProfile()));

        client.broadcastEvent(new UserUpdateEvent(asUser));
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) {
        try {
            if (message.startsWith("/")) {
                SendChatCommandResult result = new TrovoSendChatCommandRequest(
                    (TrovoUserAuth) auth,
                    client.getSimpleProfile().getChannelId(),
                    message
                )
                    .send();

                // If Trovo sends a message back to us then we need to send that to the
                // streamer.
                if ((result.getErrorMessage() != null) && !result.getErrorMessage().isEmpty()) {
                    boolean isError = !result.isSuccess();

                    client.broadcastEvent(
                        new PlatformMessageEvent(
                            result.getErrorMessage(),
                            UserPlatform.TROVO,
                            client.getProfile(),
                            isError
                        )
                    );
                }
            } else {
                new TrovoSendChatMessageRequest((TrovoUserAuth) auth, message)
                    .setChannelId(client.getSimpleProfile().getChannelId())
                    .send();
            }
        } catch (ApiAuthException e) {
            client.notifyCredentialExpired();
        } catch (Exception ignored) {}
    }

    @Override
    public void deleteMessage(@NonNull Client client, @NonNull String koiMessageId, @NonNull ClientAuthProvider auth) throws UnsupportedOperationException {
        try {
            String[] split = koiMessageId.split(":");
            String messageId = split[1];
            String senderId = split[2];
            String channelId = client.getSimpleProfile().getChannelId();

            new TrovoDeleteChatMessageRequest((TrovoUserAuth) auth, channelId, messageId, senderId)
                .send();
        } catch (ApiAuthException e) {
            client.notifyCredentialExpired();
        } catch (Exception ignored) {}
    }

    private static ConnectionHolder getProfileUpdater(Client client, TrovoUserAuth trovoAuth) {
        String channelId = trovoAuth.getSimpleProfile().getChannelId();
        ConnectionHolder holder = new ConnectionHolder(channelId, trovoAuth.getSimpleProfile());

        holder.setConn(new RepeatingThread("Trovo profile updater " + channelId, TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                holder.updateProfile(getProfile(trovoAuth));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        }));

        return holder;
    }

    public static User getProfile(TrovoUserAuth trovoAuth) throws ApiAuthException, ApiException {
        TrovoGetChannelInfoRequest infoRequest = new TrovoGetChannelInfoRequest(trovoAuth);
        TrovoGetSelfInfoRequest selfRequest = new TrovoGetSelfInfoRequest(trovoAuth);

        TrovoChannelInfo channel = infoRequest.send();
        TrovoSelfInfo self = selfRequest.send();

        User user = new User(UserPlatform.TROVO);

        // Trovo docs say the user id and channel id are the same.
        user.setIdAndChannelId(self.getUserId());

        user.setUsername(self.getUsername());
        user.setDisplayname(self.getNickname());
        user.setImageLink(self.getProfilePictureLink());

        user.setSubCount(channel.getSubscribers());
        user.setFollowersCount(channel.getFollowers());

        user.calculateColorFromUsername();

        return user;
    }

}
