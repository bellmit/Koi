package co.casterlabs.koi.integration.twitch.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.Puppet;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.integration.twitch.connections.Twitch4JAdapter;
import co.casterlabs.koi.integration.twitch.connections.TwitchMessages;
import co.casterlabs.koi.integration.twitch.connections.TwitchPubSubAdapter;
import co.casterlabs.koi.integration.twitch.connections.TwitchPuppetMessages;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
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

    // TODO Clean this up entirely and make it similar to the
    // other Integrations, this is fine for now.
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

            client.addConnection(getMessages(client, profile, twitchAuth));
            client.addConnection(getFollowers(client, profile));
            client.addConnection(getStream(client, profile));
            client.addConnection(getProfile(client, profile, twitchAuth));
            client.addConnection(getPubSub(client, profile, twitchAuth));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            throw new IdentifierException();
        }
    }

    @Override
    public synchronized void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        try {
            List<HelixUser> result = new HelixGetUsersRequest(TwitchIntegration.getInstance().getAppAuth())
                .addLogin(username)
                .send();

            if (result.isEmpty()) {
                throw new IdentifierException();
            } else {
                HelixUser profile = result.get(0);
                User asUser = TwitchUserConverter.transform(profile);

                client.setProfile(asUser);
                client.setSimpleProfile(asUser.getSimpleProfile());

                client.addConnection(getStream(client, profile));

                client.broadcastEvent(new UserUpdateEvent(asUser));
            }
        } catch (ApiAuthException e) {
            throw new IdentifierException();
        } catch (ApiException e) {
            // Log the error ONLY if it's not an invalid user error.
            if (!e.getMessage().contains("Invalid login names, emails or IDs in request") &&
                !e.getMessage().contains("Invalid URI encoding")) {
                e.printStackTrace();
            }

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

    private static ConnectionHolder getMessages(Client client, HelixUser profile, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.setConn(new TwitchMessages(holder, twitchAuth));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getPubSub(Client client, HelixUser profile, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":pubsub";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.setConn(TwitchPubSubAdapter.hook(holder, twitchAuth));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getFollowers(Client client, HelixUser profile) {
        String key = profile.getId() + ":followers";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.setConn(Twitch4JAdapter.hook(holder, false));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getStream(Client client, HelixUser profile) {
        String key = profile.getId() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.setConn(Twitch4JAdapter.hook(holder, true));

            cache.registerItem(key, holder);
        }

        return holder;
    }

    private static ConnectionHolder getProfile(Client client, HelixUser oldProfile, TwitchHelixAuth twitchAuth) {
        ConnectionHolder holder = new ConnectionHolder(oldProfile.getId() + ":profile", client.getSimpleProfile());

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
