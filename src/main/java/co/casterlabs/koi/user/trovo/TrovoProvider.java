package co.casterlabs.koi.user.trovo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.Client;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.trovoapi.requests.TrovoGetChannelInfoRequest;
import co.casterlabs.trovoapi.requests.TrovoGetSelfInfoRequest;
import co.casterlabs.trovoapi.requests.TrovoSendChatMessageRequest;
import co.casterlabs.trovoapi.requests.data.TrovoChannelInfo;
import co.casterlabs.trovoapi.requests.data.TrovoSelfInfo;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;

public class TrovoProvider implements UserProvider {
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public void hookWithAuth(@NonNull Client client, @NonNull KoiAuthProvider auth) throws IdentifierException {
        try {
            TrovoUserAuth trovoAuth = (TrovoUserAuth) auth;

            User profile = getProfile(trovoAuth);

            client.getConnections().add(getMessages(client, profile, trovoAuth));
            client.getConnections().add(getProfileUpdater(client, profile, trovoAuth));
            client.getConnections().add(getStreamPoller(client, profile, trovoAuth));

            client.setUsername(profile.getUsername());
            client.setUUID(profile.getUUID());
            client.broadcastEvent(new UserUpdateEvent(profile));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        throw new IdentifierException();
    }

    @Override
    public void upvote(@NonNull Client client, @NonNull String id, @NonNull KoiAuthProvider auth) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void chat(Client client, @NonNull String message, KoiAuthProvider auth) {
        TrovoSendChatMessageRequest request = new TrovoSendChatMessageRequest((TrovoUserAuth) auth, message);

        try {
            request.send();
        } catch (ApiAuthException e) {
            e.printStackTrace();
            client.notifyCredentialExpired();
        } catch (Exception ignored) {}
    }

    private static User getProfile(TrovoUserAuth trovoAuth) throws ApiAuthException, ApiException {
        TrovoGetSelfInfoRequest request = new TrovoGetSelfInfoRequest(trovoAuth);

        TrovoSelfInfo info = request.send();

        User user = new User(UserPlatform.TROVO);

        user.setUsername(info.getUsername());
        user.setDisplayname(info.getNickname());
        user.setUUID(info.getUserId());
        user.setImageLink(info.getProfilePictureLink());
        user.calculateColorFromUsername();

        return user;
    }

    private static ConnectionHolder getMessages(Client client, User profile, TrovoUserAuth trovoAuth) {
        String key = profile.getUUID() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            holder.setProfile(profile);

            try {
                holder.setCloseable(new TrovoMessages(holder, trovoAuth));
            } catch (ApiException | IOException ignored) {}

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getProfileUpdater(Client client, User profile, TrovoUserAuth trovoAuth) {
        String key = profile.getUUID() + ":profile";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            ConnectionHolder copy = holder;

            RepeatingThread thread = new RepeatingThread("Trovo profile updater " + profile.getUUID(), TimeUnit.MINUTES.toMillis(2), () -> {
                if (!copy.getClients().isEmpty()) {
                    Client c = copy.getClients().iterator().next();

                    try {
                        c.updateProfileSafe(getProfile(trovoAuth));
                    } catch (ApiAuthException e) {
                        c.notifyCredentialExpired();
                    } catch (Exception ignored) {}
                }
            });

            holder.setProfile(profile);
            holder.setCloseable(thread);

            thread.start();

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getStreamPoller(Client client, User profile, TrovoUserAuth trovoAuth) {
        String key = profile.getUUID() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            ConnectionHolder pointer = holder;

            RepeatingThread thread = new RepeatingThread("Trovo stream status poller " + profile.getUUID(), TimeUnit.MINUTES.toMillis(1), () -> {
                try {
                    TrovoGetChannelInfoRequest request = new TrovoGetChannelInfoRequest(trovoAuth);

                    TrovoChannelInfo info = request.send();

                    pointer.broadcastEvent(new StreamStatusEvent(info.isLive(), info.getStreamTitle(), pointer.getProfile()));
                } catch (ApiAuthException e) {
                    client.notifyCredentialExpired();
                } catch (Exception ignored) {}
            });

            holder.setProfile(profile);
            holder.setCloseable(thread);

            thread.start();

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

}
