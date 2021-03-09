package co.casterlabs.koi.networking;

import java.util.Collection;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.StatsReporter;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientEventListener;
import co.casterlabs.koi.client.Puppet;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.networking.incoming.ChatRequest;
import co.casterlabs.koi.networking.incoming.ChatRequest.Chatter;
import co.casterlabs.koi.networking.incoming.CredentialsRequest;
import co.casterlabs.koi.networking.incoming.PuppetLoginRequest;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UpvoteRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import co.casterlabs.koi.networking.outgoing.ClientBannerNotice;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageErrorType;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageType;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.eventapi.EventHelper;
import xyz.e3ndr.eventapi.listeners.EventListener;
import xyz.e3ndr.eventapi.listeners.EventWrapper;

@RequiredArgsConstructor
public class SocketClient implements ClientEventListener {
    private static final JsonObject keepAliveJson = new JsonObject();

    private @Getter @NonNull String clientId;
    private @NonNull WebSocket socket;
    private @NonNull Koi koi;

    private long lastPing = System.currentTimeMillis();

    private @Getter Collection<EventWrapper> wrappers = EventHelper.wrap(this).values();

    private @Getter Client client;
    private @Getter Puppet puppet;

    static {
        keepAliveJson.addProperty("disclaimer", "Made with \u2665 by Casterlabs");
    }

    /* -------------------------------- */
    /* Request related code             */
    /* -------------------------------- */

    @EventListener
    public void onUserLoginRequest(UserLoginRequest request) {
        try {
            if (this.client == null) {
                this.client = new Client(this, request.getToken());

                StatsReporter.get(this.client.getAuth().getPlatform()).registerConnection(this.client.getProfile().getUsername(), this.clientId);

                for (ClientBannerNotice notice : Koi.getInstance().getNotices()) {
                    this.sendNotice(notice);
                }
            } else {
                this.sendError(OutgoingMessageErrorType.USER_ALREADY_PRESENT, request.getNonce());
            }
        } catch (IdentifierException | PlatformException e) {
            this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, request.getNonce());
        }
    }

    @EventListener
    public void onPuppetLoginRequest(PuppetLoginRequest request) {
        try {
            if (this.client == null) {
                this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
            } else {
                if (this.puppet != null) {
                    this.puppet.close();
                }

                this.puppet = new Puppet(this.client, request.getToken());
            }
        } catch (IdentifierException | PlatformException e) {
            this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, request.getNonce());
        }
    }

    @EventListener
    public void onUpvoteRequest(UpvoteRequest request) {
        if ((this.client == null) || (this.client.getAuth() == null)) {
            this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
        } else {
            try {
                this.client.upvote(request.getMessageId());
            } catch (UnsupportedOperationException e) {
                this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
            }
        }
    }

    @EventListener
    public void onChatRequest(ChatRequest request) {
        if ((this.client == null) || (this.client.getAuth() == null)) {
            this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
        } else {
            try {
                if (request.getChatter() == Chatter.PUPPET) {
                    if (this.puppet == null) {
                        this.sendError(OutgoingMessageErrorType.PUPPET_USER_NOT_AUTHORIZED, request.getNonce());
                    } else {
                        try {
                            this.puppet.chat(request.getMessage());
                        } catch (ApiAuthException e) {
                            this.puppet.close();
                            this.puppet = null;
                            this.sendError(OutgoingMessageErrorType.PUPPET_AUTH_INVALID, request.getNonce());
                        }
                    }
                } else {
                    this.client.chat(request.getMessage());
                }
            } catch (UnsupportedOperationException e) {
                this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
            }
        }
    }

    @EventListener
    public void onUserStreamStatusRequest(UserStreamStatusRequest request) {
        try {
            if (this.client == null) {
                this.client = new Client(this, request.getUsername(), request.getPlatform());

                StatsReporter.get(request.getPlatform()).registerConnection(request.getUsername(), this.clientId);
            } else {
                this.sendError(OutgoingMessageErrorType.USER_ALREADY_PRESENT, request.getNonce());
            }
        } catch (IdentifierException e) {
            this.sendError(OutgoingMessageErrorType.USER_INVALID, request.getNonce());
        } catch (PlatformException e) {
            this.sendError(OutgoingMessageErrorType.USER_PLATFORM_INVALID, request.getNonce());
        }
    }

    @EventListener
    public void onTestEventRequest(TestEventRequest request) {
        Event e = request.getEventType().getTestEvent();

        if (e == null) {
            this.sendError(OutgoingMessageErrorType.REQUEST_CRITERIA_INVAID, request.getNonce());
        } else {
            this.sendEvent(e);
        }
    }

    @EventListener
    public void onCredentialsRequest(CredentialsRequest request) {
        if ((this.client == null) || (this.client.getAuth() == null)) {
            this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
        } else {
            try {
                JsonElement e = this.client.getCredentials();

                if (e.isJsonNull()) {
                    this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, request.getNonce());
                } else {
                    this.send(e.getAsJsonObject(), OutgoingMessageType.CREDENTIALS);
                }
            } catch (UnsupportedOperationException e) {
                this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
            }
        }
    }

    /* -------------------------------- */
    /* Connection related code          */
    /* -------------------------------- */

    public void onClose() {
        if (this.client != null) {
            StatsReporter.get(this.client.getProfile().getPlatform()).unregisterConnection(this.client.getProfile().getUsername(), this.clientId);

            this.client.close();
            this.client = null;
        }

        if (this.puppet != null) {
            this.puppet.close();
            this.puppet = null;
        }
    }

    public void sendKeepAlive() {
        if (this.isAlive()) {
            this.send(keepAliveJson, OutgoingMessageType.KEEP_ALIVE);
        }
    }

    public boolean isExpired(long current) {
        return (current - this.lastPing) > (SocketServer.KEEP_ALIVE_INTERVAL * 2);
    }

    public void onPong() {
        this.lastPing = System.currentTimeMillis();
    }

    public boolean isAlive() {
        return this.socket.isOpen();
    }

    /* -------------------------------- */
    /* Transmission related code        */
    /* -------------------------------- */

    @Override
    public void onCredentialExpired() {
        this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, null);
        this.onClose();
    }

    @Override
    public void onEvent(Event e) {
        this.sendEvent(e);
    }

    public void sendEvent(Event e) {
        if (e != null) {
            JsonObject json = new JsonObject();

            json.add("event", e.serialize());

            this.send(json, OutgoingMessageType.EVENT);
        }
    }

    public void sendSystemMessage(String message) {
        this.sendString(OutgoingMessageType.SYSTEM, "server", message, null);
    }

    public void sendNotice(ClientBannerNotice notice) {
        if ((this.client != null) && (this.client.getAuth() != null)) {
            JsonObject json = new JsonObject();

            json.add("notice", notice.getAsJson());

            this.send(json, OutgoingMessageType.NOTICE);
        }
    }

    public void sendError(OutgoingMessageErrorType error, String nonce) {
        this.sendString(OutgoingMessageType.ERROR, "error", error.name(), nonce);
    }

    public void sendWelcomeMessage() {
        this.sendString(OutgoingMessageType.WELCOME, "server", "Welcome! - Koi v" + Koi.VERSION, null);
    }

    private void send(JsonObject json, OutgoingMessageType type) {
        json.addProperty("type", type.name());

        if (this.isAlive()) {
            if (this.isAlive()) {
                this.socket.send(json.toString());
            }
        }
    }

    private void sendString(OutgoingMessageType type, String key, String value, String nonce) {
        JsonObject json = new JsonObject();

        json.addProperty(key, value);
        json.addProperty("nonce", nonce);

        this.send(json, type);
    }

}
