package co.casterlabs.koi.user.caffeine;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessages;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.viewers.CaffeineViewers;
import co.casterlabs.caffeineapi.requests.CaffeineUser;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.user.Client;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.KoiAuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserProvider;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;

public class CaffeineProvider implements UserProvider {
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public synchronized void hookWithAuth(@NonNull Client client, @NonNull KoiAuthProvider auth) throws IdentifierException {
        try {
            CaffeineAuth caffeineAuth = (CaffeineAuth) auth;
            String caid = caffeineAuth.getCaid();

            CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

            request.setCAID(caid);

            CaffeineUser profile = request.send();

            client.getConnections().add(getProfileUpdater(client, profile, caffeineAuth));
            client.getConnections().add(getMessagesConnection(client, profile, caffeineAuth));
            client.getConnections().add(getViewersConnection(client, profile, caffeineAuth));
            client.getConnections().add(getQueryConnection(client, profile));

            User asUser = CaffeineUserConverter.getInstance().transform(profile);

            asUser.setFollowersCount(profile.getFollowersCount());

            client.broadcastEvent(new UserUpdateEvent(asUser));

            for (ConnectionHolder holder : client.getConnections()) {
                if (holder.getHeldEvent() != null) {
                    if (holder.getHeldEvent() instanceof ViewerListEvent) {
                        ViewerListEvent viewerListEvent = (ViewerListEvent) holder.getHeldEvent();

                        for (User viewer : viewerListEvent.getViewers()) {
                            client.broadcastEvent(new ViewerJoinEvent(viewer, holder.getProfile()));
                        }
                    }

                    client.broadcastEvent(holder.getHeldEvent());
                }
            }
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public synchronized void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        try {
            CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

            request.setUsername(username);

            CaffeineUser profile = request.send();

            client.getConnections().add(getQueryConnection(client, profile));

            client.broadcastEvent(new UserUpdateEvent(CaffeineUserConverter.getInstance().transform(profile)));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    private static ConnectionHolder getProfileUpdater(Client client, CaffeineUser profile, CaffeineAuth caffeineAuth) {
        String key = profile.getCAID() + ":profile";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            RepeatingThread thread = new RepeatingThread("Caffeine profile updater " + profile.getCAID(), TimeUnit.MINUTES.toMillis(2), () -> {
                try {
                    CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

                    request.setCAID(profile.getCAID());

                    CaffeineUser updatedProfile = request.send();

                    User result = CaffeineUserConverter.getInstance().transform(updatedProfile);

                    result.setFollowersCount(updatedProfile.getFollowersCount());

                    client.updateProfileSafe(result);
                } catch (ApiException ignored) {}
            });

            holder = new ConnectionHolder(key);

            holder.setProfile(CaffeineUserConverter.getInstance().transform(profile));
            holder.setCloseable(thread);

            thread.start();

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getMessagesConnection(Client client, CaffeineUser profile, CaffeineAuth caffeineAuth) {
        String key = profile.getCAID() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            CaffeineMessages messages = new CaffeineMessages(profile);

            holder = new ConnectionHolder(key);

            holder.setProfile(CaffeineUserConverter.getInstance().transform(profile));
            holder.setCloseable(messages);

            messages.setAuth(caffeineAuth);
            messages.setListener(new CaffeineMessagesListenerAdapter(messages, holder));
            messages.connect();

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getViewersConnection(Client client, CaffeineUser profile, CaffeineAuth caffeineAuth) {
        String key = profile.getCAID() + ":viewers";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            CaffeineViewers viewers = new CaffeineViewers(caffeineAuth);

            holder = new ConnectionHolder(key);

            holder.setProfile(CaffeineUserConverter.getInstance().transform(profile));
            holder.setCloseable(viewers);

            viewers.setListener(new CaffeineViewersListenerAdapter(viewers, holder));
            viewers.connect();

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

    private static ConnectionHolder getQueryConnection(Client client, CaffeineUser profile) {
        String key = profile.getCAID() + ":query";

        ConnectionHolder holder = (ConnectionHolder) cache.getItemById(key);

        if (holder == null) {
            CaffeineQuery query = new CaffeineQuery(profile);

            holder = new ConnectionHolder(key);

            holder.setProfile(CaffeineUserConverter.getInstance().transform(profile));
            holder.setCloseable(query);

            query.setListener(new CaffeineQueryListenerAdapter(query, holder));
            query.connect();

            cache.registerItem(key, holder);
        }

        holder.getClients().add(client);

        return holder;
    }

}
