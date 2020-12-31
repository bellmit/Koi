package co.casterlabs.koi.networking;

import java.util.Collection;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import co.casterlabs.koi.networking.outgoing.MessageType;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.UserConnection;
import co.casterlabs.koi.user.UserListener;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.eventapi.EventHelper;
import xyz.e3ndr.eventapi.listeners.EventListener;
import xyz.e3ndr.eventapi.listeners.EventWrapper;

@RequiredArgsConstructor
public class SocketClient implements UserListener {
    private static final JsonObject keepAliveJson = new JsonObject();

    private @Getter @NonNull String clientType;
    private @NonNull WebSocket socket;
    private @NonNull Koi koi;

    private @Getter Collection<EventWrapper> wrappers = EventHelper.wrap(this).values();

    private @Getter boolean slim;
    private @Getter UserConnection user;

    static {
        keepAliveJson.addProperty("disclaimer", "Made with \u2665 by Casterlabs");
    }

    public void close() {
        if (this.user != null) {
            this.user.close();

            this.user = null;
        }
    }

    public void sendKeepAlive() {
        if (this.isAlive()) {
            this.send(keepAliveJson, MessageType.KEEP_ALIVE);
        }
    }

    public boolean isAlive() {
        return this.socket.isOpen();
    }

    @Override
    public void onEvent(Event e) {
        this.sendEvent(e);
    }

    @EventListener
    public void login(UserLoginRequest request) {
        try {
            if (this.user == null) {
                this.user = new UserConnection(this, request.getToken());
            } else {
                this.sendError(RequestError.USER_ALREADY_PRESENT);
            }
        } catch (IdentifierException e) {
            this.sendError(RequestError.AUTH_INVALID);
        }
    }

    @EventListener
    public void streamStatus(UserStreamStatusRequest request) {
        try {
            if (this.user == null) {
                this.user = new UserConnection(this, request.getUsername(), request.getPlatform());
            } else {
                this.sendError(RequestError.USER_ALREADY_PRESENT);
            }
        } catch (IdentifierException e) {
            this.sendError(RequestError.USER_INVALID);
        } catch (Exception e) {
            e.printStackTrace();
            this.sendError(RequestError.SERVER_INTERNAL_ERROR);
        }
    }

    @EventListener
    public void test(TestEventRequest request) {
        Event e = request.getTestType().getTestEvent();

        if (e == null) {
            this.sendError(RequestError.REQUEST_CRITERIA_INVAID);
        } else {
            this.sendEvent(e);
        }
    }

    private void send(JsonObject json, MessageType type) {
        json.addProperty("type", type.name());

        if (this.isAlive()) {
            if (this.isAlive()) {
                this.socket.send(json.toString());
            }
        }
    }

    private void sendString(MessageType type, String key, String value) {
        JsonObject json = new JsonObject();

        json.addProperty(key, value);

        this.send(json, type);
    }

    public void sendEvent(Event e) {
        if (e != null) {
            JsonObject json = new JsonObject();

            json.add("event", e.serialize());

            this.send(json, MessageType.EVENT);
        }
    }

    public void sendServerMessage(String message) {
        this.sendString(MessageType.SERVER, "server", message);
    }

    public void sendError(RequestError error) {
        this.sendString(MessageType.ERROR, "error", error.name());
    }

}
