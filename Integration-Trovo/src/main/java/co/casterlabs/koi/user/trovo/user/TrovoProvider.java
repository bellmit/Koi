package co.casterlabs.koi.user.trovo.user;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.koi.user.trovo.TrovoIntegration;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.trovoapi.requests.TrovoGetChannelInfoRequest;
import co.casterlabs.trovoapi.requests.TrovoGetSelfInfoRequest;
import co.casterlabs.trovoapi.requests.TrovoSendChatMessageRequest;
import co.casterlabs.trovoapi.requests.data.TrovoChannelInfo;
import co.casterlabs.trovoapi.requests.data.TrovoSelfInfo;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.watercache.WaterCache;

public class TrovoProvider implements UserProvider {
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            TrovoUserAuth trovoAuth = (TrovoUserAuth) auth;

            User asUser = getProfile(trovoAuth);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.getConnections().add(getMessages(client, asUser, trovoAuth));
            client.getConnections().add(getProfileUpdater(client, asUser, trovoAuth));
            client.getConnections().add(getStreamPoller(client, asUser));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            throw new IdentifierException();
        }
    }

    @Override
    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        User asUser = TrovoUserConverter.getInstance().getByNickname(username);

        client.setProfile(asUser);
        client.setSimpleProfile(asUser.getSimpleProfile());

        client.getConnections().add(getStreamPoller(client, asUser));

        client.broadcastEvent(new UserUpdateEvent(asUser));
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, ClientAuthProvider auth) {
        try {
            TrovoSendChatMessageRequest request = new TrovoSendChatMessageRequest((TrovoUserAuth) auth, message);

            // Trovo docs say the user id and channel id are the same.
            request.setChannelId(client.getUUID());

            request.send();
        } catch (ApiAuthException e) {
            client.notifyCredentialExpired();
        } catch (Exception ignored) {}
    }

    public static User getProfile(TrovoUserAuth trovoAuth) throws ApiAuthException, ApiException {
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
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            try {
                holder.setCloseable(new TrovoMessages(holder, trovoAuth));
            } catch (ApiException | IOException ignored) {}

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getProfileUpdater(Client client, User profile, TrovoUserAuth trovoAuth) {
        ConnectionHolder holder = new ConnectionHolder(profile.getUUID() + ":profile", client.getSimpleProfile());

        RepeatingThread thread = new RepeatingThread("Trovo profile updater " + profile.getUUID(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                holder.updateProfile(getProfile(trovoAuth));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        });

        holder.setCloseable(thread);

        holder.getClients().add(client);

        thread.start();

        return holder;
    }

    private static ConnectionHolder getStreamPoller(Client client, User profile) {
        String key = profile.getUUID() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            ConnectionHolder pointer = holder;

            RepeatingThread thread = new RepeatingThread("Trovo stream status poller " + profile.getUUID(), TimeUnit.MINUTES.toMillis(1), new Runnable() {
                private Instant streamStartedAt;

                @Override
                public void run() {
                    try {
                        TrovoGetChannelInfoRequest request = new TrovoGetChannelInfoRequest(TrovoIntegration.getInstance().getAppAuth(), profile.getUUID());

                        TrovoChannelInfo info = request.send();

                        if (info.isLive()) {
                            if (this.streamStartedAt == null) {
                                this.streamStartedAt = Instant.now();
                            }
                        } else {
                            this.streamStartedAt = null;
                        }

                        StreamStatusEvent e = new StreamStatusEvent(info.isLive(), info.getStreamTitle(), pointer.getProfile(), this.streamStartedAt);

                        pointer.setHeldEvent(e);
                        pointer.broadcastEvent(e);
                    } catch (ApiAuthException e) {
                        client.notifyCredentialExpired();
                    } catch (Exception ignored) {}
                }

            });

            holder.setCloseable(thread);

            thread.start();

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

}
