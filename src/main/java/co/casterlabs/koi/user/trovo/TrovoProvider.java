package co.casterlabs.koi.user.trovo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
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
            client.getConnections().add(getStreamPoller(client, profile));

            client.setUsername(profile.getUsername());
            client.setPlatform(UserPlatform.TROVO);
            client.setUUID(profile.getUUID());

            client.broadcastEvent(new UserUpdateEvent(profile));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        User profile = TrovoUserConverter.getInstance().getByNickname(username);

        client.getConnections().add(getStreamPoller(client, profile));

        client.setUsername(profile.getUsername());
        client.setPlatform(UserPlatform.TROVO);
        client.setUUID(profile.getUUID());

        client.broadcastEvent(new UserUpdateEvent(profile));
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, KoiAuthProvider auth) {
        TrovoSendChatMessageRequest request = new TrovoSendChatMessageRequest((TrovoUserAuth) auth, message);

        try {
            request.send();
        } catch (ApiAuthException e) {
            client.notifyCredentialExpired();
        } catch (Exception ignored) {}
    }

    private static User getProfile(TrovoUserAuth trovoAuth) throws ApiAuthException, ApiException {
        TrovoGetChannelInfoRequest infoRequest = new TrovoGetChannelInfoRequest(trovoAuth);
        TrovoGetSelfInfoRequest selfRequest = new TrovoGetSelfInfoRequest(trovoAuth);

        TrovoChannelInfo channel = infoRequest.send();
        TrovoSelfInfo self = selfRequest.send();

        User user = new User(UserPlatform.TROVO);

        user.setUsername(self.getUsername());
        user.setDisplayname(self.getNickname());
        user.setUUID(self.getUserId());
        user.setImageLink(self.getProfilePictureLink());

        user.setSubCount(channel.getSubscribers());
        user.setFollowersCount(channel.getFollowers());

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
        RepeatingThread thread = new RepeatingThread("Trovo profile updater " + profile.getUUID(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                client.updateProfileSafe(getProfile(trovoAuth));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        });

        ConnectionHolder holder = new ConnectionHolder("");

        holder.setProfile(profile);
        holder.setCloseable(thread);

        thread.start();

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getStreamPoller(Client client, User profile) {
        String key = profile.getUUID() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            ConnectionHolder pointer = holder;

            RepeatingThread thread = new RepeatingThread("Trovo stream status poller " + profile.getUUID(), TimeUnit.MINUTES.toMillis(1), () -> {
                try {
                    TrovoGetChannelInfoRequest request = new TrovoGetChannelInfoRequest((TrovoApplicationAuth) Koi.getInstance().getAuthProvider(UserPlatform.TROVO), profile.getUUID());

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
