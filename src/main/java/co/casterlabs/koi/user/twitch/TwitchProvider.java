package co.casterlabs.koi.user.twitch;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.Client;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserFollowersRequest;
import co.casterlabs.twitchapi.helix.requests.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.types.HelixUser;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;

public class TwitchProvider implements UserProvider {
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public synchronized void hookWithAuth(@NonNull Client client, @NonNull KoiAuthProvider auth) throws IdentifierException {
        try {
            TwitchTokenAuth twitchAuth = (TwitchTokenAuth) auth;

            HelixGetUsersRequest request = new HelixGetUsersRequest(twitchAuth);

            HelixUser profile = request.send().get(0);

            client.getConnections().add(getMessages(client, profile, twitchAuth));
            client.getConnections().add(getFollowers(client, profile));
            client.getConnections().add(getStream(client, profile));
            client.getConnections().add(getProfile(client, profile, twitchAuth));
            client.getConnections().add(getPubSub(client, profile, twitchAuth));

            User asUser = TwitchUserConverter.transform(profile);

            asUser.setFollowersCount(getFollowersCount(profile.getId(), twitchAuth));

            client.setUUID(profile.getId());
            client.setUsername(profile.getDisplayName());
            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public synchronized void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        try {
            HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchHelixAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

            request.addLogin(username);

            HelixUser profile = request.send().get(0);

            client.getConnections().add(getStream(client, profile));

            client.setUUID(profile.getId());
            client.setUsername(profile.getDisplayName());
            client.broadcastEvent(new UserUpdateEvent(TwitchUserConverter.transform(profile)));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void upvote(@NonNull Client client, @NonNull String id, @NonNull KoiAuthProvider auth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void chat(Client client, @NonNull String message, KoiAuthProvider auth) {
        String key = client.getUUID() + ":messages";

        ((TwitchMessages) ((ConnectionHolder) cache.getItemById(key)).getCloseable()).sendMessage(message);
    }

    private static ConnectionHolder getMessages(Client client, HelixUser profile, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            holder.setProfile(TwitchUserConverter.transform(profile));

            TwitchMessages messages = new TwitchMessages(holder, twitchAuth);

            holder.setCloseable(messages);

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getPubSub(Client client, HelixUser profile, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":pubsub";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            holder.setProfile(TwitchUserConverter.transform(profile));

            holder.setCloseable(TwitchPubSubAdapter.hook(holder, twitchAuth));

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getFollowers(Client client, HelixUser profile) {
        String key = profile.getId() + ":followers";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            holder.setProfile(TwitchUserConverter.transform(profile));
            holder.setCloseable(TwitchWebhookAdapter.hookFollowers(holder));

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getStream(Client client, HelixUser profile) {
        String key = profile.getId() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key);

            holder.setProfile(TwitchUserConverter.transform(profile));
            holder.setCloseable(TwitchWebhookAdapter.hookStream(holder));

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getProfile(Client client, HelixUser oldProfile, TwitchHelixAuth twitchAuth) {
        ConnectionHolder holder = new ConnectionHolder("Twitch profile updater " + oldProfile.getId());

        RepeatingThread thread = new RepeatingThread("Twitch profile updater " + oldProfile.getId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                HelixGetUsersRequest request = new HelixGetUsersRequest(twitchAuth);

                HelixUser profile = request.send().get(0);
                User user = TwitchUserConverter.transform(profile);

                user.setFollowersCount(getFollowersCount(profile.getId(), twitchAuth));

                client.broadcastEvent(new UserUpdateEvent(user));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        });

        holder.setProfile(TwitchUserConverter.transform(oldProfile));
        holder.setCloseable(thread);

        thread.start();

        holder.getClients().add(client);

        return holder;
    }

    public static long getFollowersCount(String id, TwitchHelixAuth twitchAuth) throws ApiAuthException, ApiException {
        HelixGetUserFollowersRequest followersRequest = new HelixGetUserFollowersRequest(id, twitchAuth);

        followersRequest.setFirst(1);

        return followersRequest.send().getTotal();
    }

}
