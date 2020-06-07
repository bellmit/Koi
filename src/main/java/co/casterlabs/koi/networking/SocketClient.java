package co.casterlabs.koi.networking;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.IdentifierException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.EventListener;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.ShareEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SocketClient implements EventListener {
    private static final JsonObject keepAliveJson = new JsonObject();

    private @Getter Set<User> users = Collections.synchronizedSet(new HashSet<>());
    private @NonNull WebSocket socket;
    private @NonNull Koi koi;

    static {
        keepAliveJson.addProperty("disclaimer", "Made with " + '\u2665' + " by Casterlabs");
    }

    public void close() {
        this.users.clear();

        for (User user : this.users) {
            user.getEventListeners().remove(this);
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

    public void add(JsonElement username, UserPlatform platform) {
        if ((username == null) || username.isJsonNull()) this.sendError(RequestError.USER_ID_INVALID);

        if (this.users.size() >= 10) {
            this.sendError(RequestError.USER_LIMIT_REACHED);
        } else {
            try {
                User user = this.koi.getUser(username.getAsString(), platform);

                this.users.add(user);

                user.getEventListeners().add(this);
                user.tryExternalHook();

                for (Event e : user.getDataEvents().values()) {
                    this.sendEvent(e);
                }
            } catch (IdentifierException e) {
                this.sendError(RequestError.USER_ID_INVALID);
            } catch (Exception e) {
                this.koi.getLogger().exception(e);
                this.sendError(RequestError.SERVER_API_ERROR);
            }
        }
    }

    public void remove(JsonElement username, UserPlatform platform) {
        if ((username == null) || username.isJsonNull()) this.sendError(RequestError.USER_ID_INVALID);

        try {
            User user = this.koi.getUser(username.getAsString(), platform);

            if (this.users.remove(user)) {
                user.getEventListeners().remove(this);
            } else {
                this.sendError(RequestError.USER_NOT_PRESENT);
            }
        } catch (IdentifierException e) {
            this.sendError(RequestError.USER_ID_INVALID);
        } catch (Exception e) {
            this.koi.getLogger().exception(e);
            this.sendError(RequestError.SERVER_INTERNAL_ERROR);
        }
    }

    private void send(JsonObject json, MessageType type) {
        json.addProperty("type", type.name());

        if (this.isAlive()) {
            Koi.getOutgoingThreadPool().submit(() -> this.socket.send(json.toString()));
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

    public void test(JsonElement username, UserPlatform platform, JsonElement test) {
        if ((username == null) || username.isJsonNull()) this.sendError(RequestError.USER_ID_INVALID);
        if ((test == null) || test.isJsonNull()) this.sendError(RequestError.REQUEST_CRITERIA_INVAID);

        try {
            User user = this.koi.getUser(username.getAsString(), platform);
            User casterlabs = this.koi.getUser("Casterlabs", platform);

            switch (test.getAsString().toUpperCase()) {
                case "ALL":
                    this.sendEvent(new DonationEvent("Have some candy!", casterlabs, user, "https://via.placeholder.com/150", "USD", 0));
                    this.sendEvent(new ShareEvent("Testing testing. 1, 2, 3!", casterlabs, user));
                    this.sendEvent(new ChatEvent("I like pancakes", casterlabs, user));
                    this.sendEvent(new FollowEvent(casterlabs, user));
                    return;

                case "DONATION":
                    this.sendEvent(new DonationEvent("Have some candy!", casterlabs, user, "https://via.placeholder.com/150", "USD", 0));
                    return;

                case "SHARE":
                    this.sendEvent(new ShareEvent("Testing testing. 1, 2, 3!", casterlabs, user));
                    return;

                case "CHAT":
                    this.sendEvent(new ChatEvent("I like pancakes", casterlabs, user));
                    return;

                case "FOLLOW":
                    this.sendEvent(new FollowEvent(casterlabs, user));
                    return;

                default:
                    this.sendError(RequestError.REQUEST_CRITERIA_INVAID);
                    return;
            }
        } catch (IdentifierException e) {
            this.sendError(RequestError.USER_ID_INVALID);
        } catch (Exception e) {
            this.koi.getLogger().exception(e);
            this.sendError(RequestError.SERVER_INTERNAL_ERROR);
        }
    }

}
