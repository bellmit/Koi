package co.casterlabs.koi.integration.caffeine.impl;

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
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.caffeineapi.types.CaffeineUser;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionCache;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.integration.caffeine.connections.CaffeineMessagesListenerAdapter;
import co.casterlabs.koi.integration.caffeine.connections.CaffeineProfileUpdater;
import co.casterlabs.koi.integration.caffeine.connections.CaffeineQueryListenerAdapter;
import co.casterlabs.koi.integration.caffeine.connections.CaffeineViewersListenerAdapter;
import co.casterlabs.koi.integration.caffeine.data.CaffeineUserConverter;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class CaffeineProvider implements UserProvider {

    private static ConnectionCache viewersConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @Override
        public Connection createConn(ConnectionHolder holder, String key, @NonNull ClientAuthProvider auth) {
            CaffeineViewers viewers = new CaffeineViewers((CaffeineIntegrationAuth) auth);
            CaffeineViewersListenerAdapter adapter = new CaffeineViewersListenerAdapter(viewers, holder);

            viewers.setListener(adapter);

            return adapter;
        }

    };

    private static ConnectionCache queryConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @Override
        public Connection createConn(ConnectionHolder holder, String key, @NonNull ClientAuthProvider auth) {
            CaffeineQuery query = new CaffeineQuery(key); // Key is username
            CaffeineQueryListenerAdapter adapter = new CaffeineQueryListenerAdapter(query, holder);

            query.setListener(adapter);

            return adapter;
        }

    };

    private static ConnectionCache messagesConnCache = new ConnectionCache(TimeUnit.MINUTES, 1) {

        @Override
        public Connection createConn(ConnectionHolder holder, String key, @NonNull ClientAuthProvider auth) {
            CaffeineMessages messages = new CaffeineMessages(auth.getSimpleProfile().getId());
            CaffeineMessagesListenerAdapter adapter = new CaffeineMessagesListenerAdapter(messages, holder);

            holder.setConn(adapter);

            messages.setAuth((CaffeineIntegrationAuth) auth);
            messages.setListener(adapter);

            return adapter;
        }

    };

    @Override
    public synchronized void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException {
        try {
            CaffeineIntegrationAuth caffeineAuth = (CaffeineIntegrationAuth) auth;
            String caid = caffeineAuth.getCaid();

            CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

            request.setCAID(caid);

            CaffeineUser profile = request.send();
            User asUser = CaffeineUserConverter.getInstance().transform(profile);

            asUser.setFollowersCount(profile.getFollowersCount());

            client.setProfile(asUser);
            client.setSimpleProfile(asUser.getSimpleProfile());

            client.addConnection(CaffeineProfileUpdater.get(client, caffeineAuth, asUser));

            client.addConnection(viewersConnCache.get(caid, caffeineAuth));
            client.addConnection(queryConnCache.get(profile.getUsername(), caffeineAuth));
            client.addConnection(messagesConnCache.get(caid, caffeineAuth));

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
            client.setSimpleProfile(new SimpleProfile(profile.getCAID(), profile.getCAID(), UserPlatform.CAFFEINE));

            client.addConnection(queryConnCache.get(profile.getUsername(), null));

            client.broadcastEvent(new UserUpdateEvent(CaffeineUserConverter.getInstance().transform(profile)));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public void upvote(@NonNull Client client, @NonNull String id, @NonNull ClientAuthProvider auth) throws UnsupportedOperationException {
        try {
            CaffeineIntegrationAuth caffeineAuth = (CaffeineIntegrationAuth) auth;
            String[] split = id.split(":", 2);

            String type = split[0].toUpperCase();
            String messageId = split[1];

            switch (type) {
                case "CHAT": {
                    CaffeineUpvoteChatMessageRequest upvoteRequest = new CaffeineUpvoteChatMessageRequest(caffeineAuth);

                    // TEMP
                    JsonObject payload = new JsonObject();

                    payload.addProperty("s", client.getSimpleProfile().getChannelId().substring(4));
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

            // See if the command is /afterparty,
            // and if it is then we change the message content
            if (message.matches("\\/afterparty (@)?[-\\w]+")) {
                String raidTarget = message.split(" ")[1];

                StringBuilder afterPartyMessage = new StringBuilder();

                afterPartyMessage.append("Head to the After Party with ");

                if (raidTarget.startsWith("@")) {
                    afterPartyMessage.append(raidTarget);
                } else {
                    afterPartyMessage
                        .append('@')
                        .append(raidTarget);
                }

                message = afterPartyMessage.toString();
            }

            CaffeineSendChatMessageRequest request = new CaffeineSendChatMessageRequest((co.casterlabs.caffeineapi.CaffeineAuth) auth);

            request.setCAID(client.getSimpleProfile().getChannelId());
            request.setMessage(message);

            try {
                request.send();
            } catch (ApiAuthException e) {
                throw e;
            } catch (ApiException ignored) {}
        }
    }

}
