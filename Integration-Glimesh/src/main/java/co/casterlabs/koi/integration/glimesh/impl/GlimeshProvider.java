package co.casterlabs.koi.integration.glimesh.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.requests.GlimeshGetChannelRequest;
import co.casterlabs.glimeshapijava.requests.GlimeshGetMyselfRequest;
import co.casterlabs.glimeshapijava.requests.GlimeshGetUserFollowersRequest;
import co.casterlabs.glimeshapijava.requests.GlimeshGetUserSubscribersRequest;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshSubscriber;
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionCache;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.integration.glimesh.connections.GlimeshChatWrapper;
import co.casterlabs.koi.integration.glimesh.connections.GlimeshFollowerWrapper;
import co.casterlabs.koi.integration.glimesh.connections.GlimeshStreamWrapper;
import co.casterlabs.koi.integration.glimesh.data.GlimeshUserConverter;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformProvider;
import co.casterlabs.koi.user.User;
import lombok.NonNull;
import lombok.SneakyThrows;

public class GlimeshProvider implements PlatformProvider {

    private static ConnectionCache streamConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            return new GlimeshStreamWrapper(holder);
        }

    };

    private static ConnectionCache messagesConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            return new GlimeshChatWrapper(holder);
        }

    };

    private static ConnectionCache followersConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @SneakyThrows
        @Override
        public Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth) {
            return new GlimeshFollowerWrapper(holder);
        }

    };

    @Override
    public void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            GlimeshUserAuth glimeshAuth = (GlimeshUserAuth) auth;

            User asUser = getProfile(glimeshAuth);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(getProfileUpdater(client, glimeshAuth));

            client.addConnection(streamConnCache.get(asUser.getChannelId(), glimeshAuth, asUser.getSimpleProfile()));
            client.addConnection(messagesConnCache.get(asUser.getChannelId(), glimeshAuth, asUser.getSimpleProfile()));
            client.addConnection(followersConnCache.get(asUser.getChannelId(), glimeshAuth, asUser.getSimpleProfile()));

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

        User asUser = GlimeshUserConverter.getInstance().get(username);

        if (asUser == null) {
            throw new IdentifierException();
        } else {
            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(streamConnCache.get(asUser.getChannelId(), null, asUser.getSimpleProfile()));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        }
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) throws ApiAuthException {
        // TODO Figure out why chat is broken.
        /* try {
            GlimeshSendChatMessageRequest request = new GlimeshSendChatMessageRequest((GlimeshAuth) auth, message, auth.getSimpleProfile().tryGetChannelIdAsInt());
        
            request.send();
        } catch (ApiAuthException e) {
            throw e;
        } catch (ApiException e) {
            e.printStackTrace();
        } */
    }

    // We don't register this one.
    private static ConnectionHolder getProfileUpdater(Client client, GlimeshUserAuth glimeshAuth) {
        String channelId = glimeshAuth.getSimpleProfile().getChannelId();
        ConnectionHolder holder = new ConnectionHolder(channelId, glimeshAuth.getSimpleProfile());

        holder.setConn(new co.casterlabs.koi.util.RepeatingThread("Glimesh profile updater " + channelId, TimeUnit.MINUTES.toMillis(5), () -> {
            try {
                glimeshAuth.refresh();

                User asUser = getProfile(glimeshAuth);

                holder.updateProfile(asUser);
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        }));

        return holder;
    }

    public static User getProfile(GlimeshUserAuth glimeshAuth) throws ApiAuthException, ApiException {
        GlimeshUser glimeshUser = new GlimeshGetMyselfRequest(glimeshAuth).send();
        GlimeshChannel glimeshChannel = new GlimeshGetChannelRequest(glimeshAuth, glimeshUser.getUsername()).send();

        User asUser = GlimeshUserConverter.getInstance().transform(glimeshUser);

        asUser.setChannelId(String.valueOf(glimeshChannel.getId()));

        int followersCount = new GlimeshGetUserFollowersRequest(glimeshAuth, glimeshUser.getUsername()).send().size();
        int subCount = 0;

        // Terrible way to get sub count
        List<GlimeshSubscriber> subscribers = new GlimeshGetUserSubscribersRequest(glimeshAuth, glimeshUser.getUsername()).send();

        for (GlimeshSubscriber sub : subscribers) {
            if (sub.isActive()) {
                subCount++;
            }
        }

        asUser.setFollowersCount(followersCount);
        asUser.setSubCount(subCount);

        return asUser;
    }

}
