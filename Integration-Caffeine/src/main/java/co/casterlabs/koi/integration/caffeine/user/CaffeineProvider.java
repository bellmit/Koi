package co.casterlabs.koi.integration.caffeine.user;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessages;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.viewers.CaffeineViewers;
import co.casterlabs.caffeineapi.requests.CaffeineSendChatMessageRequest;
import co.casterlabs.caffeineapi.requests.CaffeineUpvoteChatMessageRequest;
import co.casterlabs.caffeineapi.requests.CaffeineUser;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.watercache.WaterCache;

public class CaffeineProvider implements UserProvider {
    private static WaterCache connectionCache = new WaterCache();

    static {
        connectionCache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public synchronized void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            CaffeineAuth caffeineAuth = (CaffeineAuth) auth;
            String caid = caffeineAuth.getCaid();

            CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

            request.setCAID(caid);

            CaffeineUser profile = request.send();
            User asUser = CaffeineUserConverter.getInstance().transform(profile);

            asUser.setFollowersCount(profile.getFollowersCount());

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.getConnections().add(CaffeineProfileUpdater.get(client, caffeineAuth));
            client.getConnections().add(getMessagesConnection(client, profile, caffeineAuth));
            client.getConnections().add(getViewersConnection(client, profile, caffeineAuth));
            client.getConnections().add(getQueryConnection(client, profile));

            client.broadcastEvent(new UserUpdateEvent(asUser));
        } catch (ApiException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            throw new IdentifierException();
        }
    }

    @Override
    public synchronized void hook(@NonNull Client client, @NonNull String username) throws IdentifierException {
        try {
            CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

            request.setUsername(username);

            CaffeineUser profile = request.send();
            User asUser = CaffeineUserConverter.getInstance().transform(profile);

            asUser.setFollowersCount(profile.getFollowersCount());

            client.setProfile(asUser);
            client.setSimpleProfile(new SimpleProfile(profile.getCAID(), UserPlatform.CAFFEINE));

            client.getConnections().add(getQueryConnection(client, profile));

            client.broadcastEvent(new UserUpdateEvent(CaffeineUserConverter.getInstance().transform(profile)));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void upvote(@NonNull Client client, @NonNull String id, @NonNull ClientAuthProvider auth) throws UnsupportedOperationException {
        try {
            CaffeineAuth caffeineAuth = (CaffeineAuth) auth;
            String[] split = id.split(":", 2);

            String type = split[0].toUpperCase();
            String messageId = split[1];

            switch (type) {
                case "CHAT": {
                    CaffeineUpvoteChatMessageRequest upvoteRequest = new CaffeineUpvoteChatMessageRequest(caffeineAuth);

                    // TEMP
                    JsonObject payload = new JsonObject();

                    payload.addProperty("s", client.getUUID().substring(4));
                    payload.addProperty("u", messageId);

                    String encoded = Base64.getEncoder().encodeToString(payload.toString().getBytes());

                    upvoteRequest.setMessageId(encoded);

                    try {
                        upvoteRequest.send();
                    } catch (ApiAuthException e) {
                        client.notifyCredentialExpired();
                    } catch (ApiException e) {
                        throw new UnsupportedOperationException();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void chat(Client client, @NonNull String message, ClientAuthProvider auth) throws ApiAuthException {
        if (message.length() <= 80) {
            CaffeineSendChatMessageRequest request = new CaffeineSendChatMessageRequest((co.casterlabs.caffeineapi.CaffeineAuth) auth);

            request.setCAID(client.getUUID());
            request.setMessage(message);

            try {
                request.send();
            } catch (ApiAuthException e) {
                throw e;
            } catch (ApiException ignored) {}
        }
    }

    private static ConnectionHolder getMessagesConnection(Client client, CaffeineUser profile, CaffeineAuth caffeineAuth) {
        String key = profile.getCAID() + ":messages";

        ConnectionHolder holder = (ConnectionHolder) connectionCache.getItemById(key);

        if (holder == null) {
            CaffeineMessages messages = new CaffeineMessages(profile);

            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(messages);

            messages.setAuth(caffeineAuth);
            messages.setListener(new CaffeineMessagesListenerAdapter(messages, holder));
            messages.connect();

            connectionCache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getViewersConnection(Client client, CaffeineUser profile, CaffeineAuth caffeineAuth) {
        String key = profile.getCAID() + ":viewers";

        ConnectionHolder holder = (ConnectionHolder) connectionCache.getItemById(key);

        if (holder == null) {
            CaffeineViewers viewers = new CaffeineViewers(caffeineAuth);

            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(viewers);

            viewers.setListener(new CaffeineViewersListenerAdapter(viewers, holder));
            viewers.connect();

            connectionCache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

    private static ConnectionHolder getQueryConnection(Client client, CaffeineUser profile) {
        String key = profile.getCAID() + ":query";

        ConnectionHolder holder = (ConnectionHolder) connectionCache.getItemById(key);

        if (holder == null) {
            CaffeineQuery query = new CaffeineQuery(profile);

            holder = new ConnectionHolder(key, client.getSimpleProfile());

            holder.getClients().add(client);

            holder.setCloseable(query);

            query.setListener(new CaffeineQueryListenerAdapter(query, holder));
            query.connect();

            connectionCache.registerItem(key, holder);
        } else {
            holder.getClients().add(client);
        }

        return holder;
    }

}
