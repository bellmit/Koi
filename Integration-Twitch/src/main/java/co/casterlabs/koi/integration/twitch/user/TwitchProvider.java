package co.casterlabs.koi.integration.twitch.user;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.Puppet;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserFollowersRequest;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserSubscribersRequest;
import co.casterlabs.twitchapi.helix.requests.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.types.HelixUser;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.watercache.WaterCache;

public class TwitchProvider implements PlatformProvider {
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public synchronized void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            TwitchTokenAuth twitchAuth = (TwitchTokenAuth) auth;

            HelixGetUsersRequest request = new HelixGetUsersRequest(twitchAuth);

            HelixUser profile = request.send().get(0);
            User asUser = TwitchUserConverter.transform(profile);

            asUser.setFollowersCount(getFollowersCount(profile.getId(), twitchAuth));
            asUser.setSubCount(getSubscribersCount(profile.getId(), twitchAuth));

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(getMessages(client, profile, asUser, twitchAuth));
            client.addConnection(getFollowers(client, profile, asUser));
            client.addConnection(getStream(client, profile, asUser));
            client.addConnection(getProfile(client, profile, asUser, twitchAuth));
            client.addConnection(getPubSub(client, profile, asUser, twitchAuth));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            throw new IdentifierException();
        }
    }

    @Override
    public synchronized void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        try {
            HelixGetUsersRequest request = new HelixGetUsersRequest(TwitchIntegration.getInstance().getAppAuth());

            request.addLogin(username);

            HelixUser profile = request.send().get(0);
            User asUser = TwitchUserConverter.transform(profile);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(getStream(client, profile, asUser));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (IndexOutOfBoundsException | ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) {
        String key = client.getSimpleProfile().getChannelId() + ":messages";

        ((TwitchMessages) ((ConnectionHolder) cache.getItemById(key)).getConn()).sendMessage(message);
    }

    @Override
    public void initializePuppet(@NonNull Puppet puppet) throws ApiAuthException {
        TwitchPuppetMessages messages = new TwitchPuppetMessages(puppet, (TwitchTokenAuth) puppet.getAuth());

        puppet.setCloseable(messages);
    }

    @Override
    public void chatAsPuppet(@NonNull Puppet puppet, @NonNull String message) throws UnsupportedOperationException, ApiAuthException {
        TwitchMessages messages = (TwitchMessages) puppet.getCloseable();

        messages.sendMessage(message);
    }

    private static ConnectionHolder getMessages(Client client, HelixUser profile, User asUser, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, asUser);

            holder.setConn(new TwitchMessages(holder, twitchAuth));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getPubSub(Client client, HelixUser profile, User asUser, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":pubsub";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, asUser);

            holder.setConn(TwitchPubSubAdapter.hook(holder, twitchAuth));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getFollowers(Client client, HelixUser profile, User asUser) {
        String key = profile.getId() + ":followers";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, asUser);

            holder.setConn(TwitchWebhookAdapter.hookFollowers(holder));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getStream(Client client, HelixUser profile, User asUser) {
        String key = profile.getId() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, asUser);

            holder.setConn(TwitchWebhookAdapter.hookStream(holder));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getProfile(Client client, HelixUser oldProfile, User asUser, TwitchHelixAuth twitchAuth) {
        ConnectionHolder holder = new ConnectionHolder(oldProfile.getId() + ":profile", asUser);

        holder.setConn(new RepeatingThread("Twitch Profile Updater " + oldProfile.getId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                HelixUser profile = new HelixGetUsersRequest(twitchAuth).send().get(0);

                User user = TwitchUserConverter.transform(profile);

                user.setFollowersCount(getFollowersCount(profile.getId(), twitchAuth));
                user.setSubCount(getSubscribersCount(profile.getId(), twitchAuth));

                client.broadcastEvent(new UserUpdateEvent(user));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        }));

        return holder;
    }

    public static long getFollowersCount(String id, TwitchHelixAuth twitchAuth) throws ApiAuthException, ApiException {
        HelixGetUserFollowersRequest followersRequest = new HelixGetUserFollowersRequest(id, twitchAuth);

        followersRequest.setFirst(1);

        return followersRequest.send().getTotal();
    }

    public static long getSubscribersCount(String id, TwitchHelixAuth twitchAuth) throws ApiAuthException, ApiException {
        try {
            return new HelixGetUserSubscribersRequest(id, twitchAuth).send().size();
        } catch (ApiException e) {
            if (e.getMessage().contains("channel_id must be a partner or affiliate")) {
                return -1;
            } else {
                throw e;
            }
        }
    }

}
