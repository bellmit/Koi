package co.casterlabs.koi.user.twitch;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserFollowersRequest;
import co.casterlabs.twitchapi.helix.requests.HelixGetUserSubscribersRequest;
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

            client.getConnections().add(getMessages(client, profile, twitchAuth));
            client.getConnections().add(getFollowers(client, profile));
            client.getConnections().add(getStream(client, profile));
            client.getConnections().add(getProfile(client, profile, twitchAuth));
            client.getConnections().add(getPubSub(client, profile, twitchAuth));

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
            User asUser = TwitchUserConverter.transform(profile);

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.getConnections().add(getStream(client, profile));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ArrayIndexOutOfBoundsException | ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void chat(Client client, @NonNull String message, ClientAuthProvider auth) {
        String key = client.getUUID() + ":messages";

        ((TwitchMessages) ((ConnectionHolder) cache.getItemById(key)).getCloseable()).sendMessage(message);
    }

    private static ConnectionHolder getMessages(Client client, HelixUser profile, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            // Special case, since we need the username off the bat.
            holder.getClients().add(client);

            TwitchMessages messages = new TwitchMessages(holder, twitchAuth);

            holder.setCloseable(messages);

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getPubSub(Client client, HelixUser profile, TwitchTokenAuth twitchAuth) {
        String key = profile.getId() + ":pubsub";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(TwitchPubSubAdapter.hook(holder, twitchAuth));

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getFollowers(Client client, HelixUser profile) {
        String key = profile.getId() + ":followers";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(TwitchWebhookAdapter.hookFollowers(holder));

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getStream(Client client, HelixUser profile) {
        String key = profile.getId() + ":stream";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(TwitchWebhookAdapter.hookStream(holder));

            cache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getProfile(Client client, HelixUser oldProfile, TwitchHelixAuth twitchAuth) {
        ConnectionHolder holder = new ConnectionHolder(oldProfile.getId() + ":profile", client.getSimpleProfile());

        RepeatingThread thread = new RepeatingThread("Twitch Profile Updater " + oldProfile.getId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                HelixUser profile = new HelixGetUsersRequest(twitchAuth).send().get(0);

                User user = TwitchUserConverter.transform(profile);

                user.setFollowersCount(getFollowersCount(profile.getId(), twitchAuth));
                user.setSubCount(getSubscribersCount(profile.getId(), twitchAuth));

                client.broadcastEvent(new UserUpdateEvent(user));
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        });

        holder.setCloseable(thread);

        holder.getClients().add(client);

        thread.start();

        return holder;
    }

    public static long getFollowersCount(String id, TwitchHelixAuth twitchAuth) throws ApiAuthException, ApiException {
        HelixGetUserFollowersRequest followersRequest = new HelixGetUserFollowersRequest(id, twitchAuth);

        followersRequest.setFirst(1);

        return followersRequest.send().getTotal();
    }

    public static long getSubscribersCount(String id, TwitchHelixAuth twitchAuth) throws ApiAuthException, ApiException {
        return new HelixGetUserSubscribersRequest(id, twitchAuth).send().size();
    }

}
