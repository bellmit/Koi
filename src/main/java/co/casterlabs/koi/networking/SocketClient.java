package co.casterlabs.koi.networking;

import java.util.Collection;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.StatsReporter;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.networking.incoming.ChatRequest;
import co.casterlabs.koi.networking.incoming.CredentialsRequest;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UpvoteRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import co.casterlabs.koi.networking.outgoing.ResponseType;
import co.casterlabs.koi.user.Client;
import co.casterlabs.koi.user.ClientEventListener;
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

    private @Getter @NonNull String clientType;
    private @NonNull WebSocket socket;
    private @NonNull Koi koi;

    private @Getter Collection<EventWrapper> wrappers = EventHelper.wrap(this).values();

    private @Getter Client client;

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

                StatsReporter.get(this.client.getAuth().getPlatform()).registerConnection(this.client.getUsername(), this.clientType);
            } else {
                this.sendError(RequestError.USER_ALREADY_PRESENT, request.getNonce());
            }
        } catch (IdentifierException | PlatformException e) {
            this.sendError(RequestError.AUTH_INVALID, request.getNonce());
        }
    }

    @EventListener
    public void onUpvoteRequest(UpvoteRequest request) {
        if ((this.client == null) || (this.client.getAuth() == null)) {
            this.sendError(RequestError.USER_NOT_AUTHORIZED, request.getNonce());
        } else {
            try {
                this.client.upvote(request.getMessageId());
            } catch (UnsupportedOperationException e) {
                this.sendError(RequestError.NOT_IMPLEMENTED, request.getNonce());
            }
        }
    }

    @EventListener
    public void onChatRequest(ChatRequest request) {
        if ((this.client == null) || (this.client.getAuth() == null)) {
            this.sendError(RequestError.USER_NOT_AUTHORIZED, request.getNonce());
        } else {
            try {
                this.client.chat(request.getMessage());
            } catch (UnsupportedOperationException e) {
                this.sendError(RequestError.NOT_IMPLEMENTED, request.getNonce());
            }
        }
    }

    @EventListener
    public void onUserStreamStatusRequest(UserStreamStatusRequest request) {
        try {
            if (this.client == null) {
                this.client = new Client(this, request.getUsername(), request.getPlatform());

                StatsReporter.get(request.getPlatform()).registerConnection(this.client.getUsername(), this.clientType);
            } else {
                this.sendError(RequestError.USER_ALREADY_PRESENT, request.getNonce());
            }
        } catch (IdentifierException e) {
            this.sendError(RequestError.USER_INVALID, request.getNonce());
        } catch (PlatformException e) {
            this.sendError(RequestError.USER_PLATFORM_INVALID, request.getNonce());
        }
    }

    @EventListener
    public void onTestEventRequest(TestEventRequest request) {
        Event e = request.getEventType().getTestEvent();

        if (e == null) {
            this.sendError(RequestError.REQUEST_CRITERIA_INVAID, request.getNonce());
        } else {
            this.sendEvent(e);
        }
    }

    @EventListener
    public void onCredentialsRequest(CredentialsRequest request) {
        if ((this.client == null) || (this.client.getAuth() == null)) {
            this.sendError(RequestError.USER_NOT_AUTHORIZED, request.getNonce());
        } else {
            JsonElement e = this.client.getCredentials();

            if (e.isJsonNull()) {
                this.sendError(RequestError.AUTH_INVALID, request.getNonce());
            } else {
                this.send(e.getAsJsonObject(), ResponseType.CREDENTIALS);
            }
        }
    }

    /* -------------------------------- */
    /* Connection related code          */
    /* -------------------------------- */

    public void close() {
        if (this.client != null) {
            StatsReporter.get(this.client.getAuth().getPlatform()).unregisterConnection(this.client.getUsername(), this.clientType);

            this.client.close();

            this.client = null;
        }
    }

    public void sendKeepAlive() {
        if (this.isAlive()) {
            this.send(keepAliveJson, ResponseType.KEEP_ALIVE);
        }
    }

    public boolean isAlive() {
        return this.socket.isOpen();
    }

    /* -------------------------------- */
    /* Transmission related code        */
    /* -------------------------------- */

    @Override
    public void onCredentialExpired() {
        this.sendError(RequestError.AUTH_INVALID, null);
        this.close();
    }

    @Override
    public void onEvent(Event e) {
        this.sendEvent(e);
    }

    public void sendEvent(Event e) {
        if (e != null) {
            JsonObject json = new JsonObject();

            json.add("event", e.serialize());

            this.send(json, ResponseType.EVENT);
        }
    }

    public void sendSystemMessage(String message) {
        this.sendString(ResponseType.SYSTEM, "server", message, null);
    }

    public void sendError(RequestError error, String nonce) {
        this.sendString(ResponseType.ERROR, "error", error.name(), nonce);
    }

    public void sendWelcomeMessage() {
        this.sendString(ResponseType.WELCOME, "server", "Welcome! - Koi v" + Koi.VERSION, null);
    }

    private void send(JsonObject json, ResponseType type) {
        json.addProperty("type", type.name());

        if (this.isAlive()) {
            if (this.isAlive()) {
                this.socket.send(json.toString());
            }
        }
    }

    private void sendString(ResponseType type, String key, String value, String nonce) {
        JsonObject json = new JsonObject();

        json.addProperty(key, value);
        json.addProperty("nonce", nonce);

        this.send(json, type);
    }

}
