package co.casterlabs.koi.user.trovo.user;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
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

public class TrovoProvider implements PlatformProvider {
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

            client.addConnection(getMessages(client, asUser, trovoAuth));
            client.addConnection(getProfileUpdater(client, asUser, trovoAuth));
            client.addConnection(getStreamPoller(client, asUser));

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

        client.addConnection(getStreamPoller(client, asUser));

        client.broadcastEvent(new UserUpdateEvent(asUser));
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) {
        try {
            TrovoSendChatMessageRequest request = new TrovoSendChatMessageRequest((TrovoUserAuth) auth, message);

            request.setChannelId(client.getSimpleProfile().getChannelId());

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

    private static ConnectionHolder getMessages(Client client, User profile, TrovoUserAuth trovoAuth) {
        String key = profile.getChannelId() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, profile);

            try {
                holder.setConn(new TrovoMessages(holder, trovoAuth));
            } catch (ApiException | IOException ignored) {}

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getProfileUpdater(Client client, User profile, TrovoUserAuth trovoAuth) {
        ConnectionHolder holder = new ConnectionHolder(profile.getChannelId() + ":profile", profile);

        holder.setConn(new RepeatingThread("Trovo profile updater " + profile.getChannelId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                holder.updateProfile(getProfile(trovoAuth));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        }));

        return holder;
    }

    private static ConnectionHolder getStreamPoller(Client client, User profile) {
        String key = profile.getChannelId() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, profile);

            ConnectionHolder pointer = holder;

            holder.setConn(new RepeatingThread("Trovo stream status poller " + profile.getChannelId(), TimeUnit.MINUTES.toMillis(1), new Runnable() {
                private Instant streamStartedAt;

                @Override
                public void run() {
                    try {
                        TrovoGetChannelInfoRequest request = new TrovoGetChannelInfoRequest(TrovoIntegration.getInstance().getAppAuth(), profile.getChannelId());

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

            }));

            cache.registerItem(key, holder);
        }

        return holder;
    }

}
