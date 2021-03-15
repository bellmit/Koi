package co.casterlabs.koi.user.glimesh;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.requests.GlimeshGetMyselfRequest;
import co.casterlabs.glimeshapijava.requests.GlimeshSendChatMessageRequest;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserProvider;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;

public class GlimeshProvider implements UserProvider {
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            GlimeshUserAuth glimeshAuth = (GlimeshUserAuth) auth;

            User asUser = getProfile(glimeshAuth);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.getConnections().add(getStream(client, asUser));
            client.getConnections().add(getMessages(client, asUser));
            client.getConnections().add(getProfileUpdater(client, asUser, glimeshAuth));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        try {
            User asUser = GlimeshUserConverter.getInstance().get(username);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.getConnections().add(getStream(client, asUser));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) throws ApiAuthException {
        try {
            GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(client.getProfile().getUsername());
            GlimeshSendChatMessageRequest request = new GlimeshSendChatMessageRequest((GlimeshAuth) auth, message, channel.getId());

            request.send();
        } catch (ApiAuthException e) {
            throw e;
        } catch (ApiException ignored) {}
    }

    private static ConnectionHolder getMessages(Client client, User profile) throws ApiAuthException, ApiException {
        String key = profile.getUUID() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(new GlimeshChatWrapper(holder));

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getStream(Client client, User profile) throws ApiAuthException, ApiException {
        String key = profile.getUUID() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(new GlimeshStreamWrapper(holder));

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    // We don't register this one.
    private static ConnectionHolder getProfileUpdater(Client client, User profile, GlimeshUserAuth glimeshAuth) {
        ConnectionHolder holder = new ConnectionHolder(profile.getUUID() + ":profile", client.getSimpleProfile());

        RepeatingThread thread = new RepeatingThread("Glimesh profile updater " + profile.getUUID(), TimeUnit.MINUTES.toMillis(10), () -> {
            try {
                glimeshAuth.refresh();

                User asUser = getProfile(glimeshAuth);
//                GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(asUser.getUsername());
//
//                asUser.setFollowersCount(channel.);
//                
                holder.updateProfile(asUser);
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        });

        holder.setCloseable(thread);

        holder.getClients().add(client);

        thread.start();

        return holder;
    }

    private static User getProfile(GlimeshUserAuth glimeshAuth) throws ApiAuthException, ApiException {
        GlimeshGetMyselfRequest request = new GlimeshGetMyselfRequest(glimeshAuth);

        GlimeshUser glimesh = request.send();

        return GlimeshUserConverter.getInstance().transform(glimesh);
    }

}
