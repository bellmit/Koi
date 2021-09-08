package co.casterlabs.koi.integration.brime.impl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.brimeapijava.realtime.BrimeChat;
import co.casterlabs.brimeapijava.requests.BrimeGetAccountRequest;
import co.casterlabs.brimeapijava.requests.BrimeGetChannelRequest;
import co.casterlabs.brimeapijava.types.BrimeAccount;
import co.casterlabs.brimeapijava.types.BrimeChannel;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionCache;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.integration.brime.connections.BrimeChatAdapter;
import co.casterlabs.koi.integration.brime.data.BrimeUserConverter;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.RepeatingThread;
import lombok.NonNull;
import lombok.SneakyThrows;

public class BrimeProvider implements PlatformProvider {

    private static ConnectionCache chatConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            BrimeChannel channel = new BrimeGetChannelRequest()
                .queryBySlug(auth.getSimpleProfile().getChannelId())
                .send();

            BrimeChat chat = new BrimeChat(channel, (BrimeUserAuth) auth);
            BrimeChatAdapter adapter = new BrimeChatAdapter(holder, chat);

            chat.setListener(adapter);

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
                        BrimeChannel channel = new BrimeGetChannelRequest()
                            .queryByXid(channelId)
                            .send();

                        boolean isLive = channel.getChannel().isLive();

                        this.title = channel.getStream().getTitle();

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

            client.addConnection(chatConnCache.get(asUser.getChannelId(), auth, asUser.getSimpleProfile()));
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
//        try {
//            new BrimeSendChatMessageRequest((BrimeUserAuth) auth)
//                .setChannelId(client.getSimpleProfile().getChannelId())
//                .setColor("#ea4c4c")
//                .setMessage(message)
//                .send();
//        } catch (ApiAuthException e) {
//            throw e;
//        } catch (ApiException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void deleteMessage(@NonNull Client client, @NonNull String messageId, @NonNull ClientAuthProvider auth) {
//        try {
//            new BrimeDeleteChatMessageRequest((BrimeUserAuth) auth)
//                .setChannelId(client.getSimpleProfile().getChannelId())
//                .setMessageId(messageId)
//                .send();
//        } catch (ApiException ignored) {}
    }

    private static ConnectionHolder getProfileUpdater(Client client, BrimeUserAuth brimeAuth) {
        ConnectionHolder holder = new ConnectionHolder(brimeAuth.getSimpleProfile().getChannelId(), brimeAuth.getSimpleProfile());

        holder.setConn(new RepeatingThread("Brime profile updater " + brimeAuth.getSimpleProfile().getChannelId(), TimeUnit.MINUTES.toMillis(2), () -> {
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
        BrimeChannel channel = new BrimeGetChannelRequest()
            .queryByXid(brimeAuth.getSimpleProfile().getChannelId())
            .send();

        BrimeAccount user = new BrimeGetAccountRequest(brimeAuth)
            .send();

        User asUser = new User(UserPlatform.BRIME);

        asUser.setImageLink(String.format("https://content.brimecdn.com/brime/user/604e1cf62cbb31a8fe5e1de5/avatar", user.getXid()));

        asUser.setDisplayname(user.getDisplayname());
        asUser.setUsername(user.getUsername());
        asUser.setId(user.getXid());
//        asUser.setBadges(new HashSet<>(user.getBadges()));
//        asUser.setRoles(roles); // TODO
//        asUser.setColor(user.getColor());

        asUser.setChannelId(channel.getChannel().getXid());

//        asUser.setFollowersCount(channel.getFollowerCount());
//        asUser.setSubCount(channel.getSubscriberCount());

        return asUser;
    }

}
