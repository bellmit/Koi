package co.casterlabs.koi.networking;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.networking.incoming.ChatRequest;
import co.casterlabs.koi.networking.incoming.CredentialsRequest;
import co.casterlabs.koi.networking.incoming.DeleteMyDataRequest;
import co.casterlabs.koi.networking.incoming.IncomingMessageType;
import co.casterlabs.koi.networking.incoming.PuppetLoginRequest;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UpvoteRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import co.casterlabs.koi.networking.outgoing.ClientBannerNotice;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageErrorType;
import co.casterlabs.koi.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.eventapi.events.AbstractEvent;
import xyz.e3ndr.eventapi.events.deserializer.GsonEventDeserializer;
import xyz.e3ndr.eventapi.listeners.EventWrapper;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class SocketServer extends WebSocketServer implements Server {
    public static final long KEEP_ALIVE_INTERVAL = TimeUnit.SECONDS.toMillis(10);

    private static GsonEventDeserializer<IncomingMessageType> eventDeserializer = new GsonEventDeserializer<>();

    private static @Getter SocketServer instance;

    private RepeatingThread thread = new RepeatingThread("Keep Alive - Koi", KEEP_ALIVE_INTERVAL, () -> this.keepAllAlive());
    private @Getter boolean running = false;
    private Koi koi;

    static {
        eventDeserializer.registerEventClass(IncomingMessageType.USER_STREAM_STATUS, UserStreamStatusRequest.class);
        eventDeserializer.registerEventClass(IncomingMessageType.PUPPET_LOGIN, PuppetLoginRequest.class);
        eventDeserializer.registerEventClass(IncomingMessageType.LOGIN, UserLoginRequest.class);

        eventDeserializer.registerEventClass(IncomingMessageType.DELETE_MY_DATA, DeleteMyDataRequest.class);
        eventDeserializer.registerEventClass(IncomingMessageType.TEST, TestEventRequest.class);

        eventDeserializer.registerEventClass(IncomingMessageType.CREDENTIALS, CredentialsRequest.class);
        eventDeserializer.registerEventClass(IncomingMessageType.UPVOTE, UpvoteRequest.class);
        eventDeserializer.registerEventClass(IncomingMessageType.CHAT, ChatRequest.class);
    }

    public SocketServer(InetSocketAddress bind, Koi koi) {
        super(bind);

        if (instance == null) {
            instance = this;
        }

        this.koi = koi;
    }

    private void keepAllAlive() {
        if (this.running) {
            long current = System.currentTimeMillis();

            for (WebSocket conn : this.getConnections()) {
                SocketClient client = conn.getAttachment();

                if (client.isExpired(current)) {
                    client.sendError(OutgoingMessageErrorType.FAILED_KEEP_ALIVE, null);
                    client.onClose();
                    conn.close();
                } else {
                    client.sendKeepAlive();
                }
            }
        } else {
            this.thread.stop();
        }
    }

    @SneakyThrows
    @Override
    public void stop() {
        this.running = false;
        super.stop();
    }

    @Override
    public void start() {
        super.start();

        Koi.getInstance().getLogger().info("Koi started on %s:%d!", this.getAddress().getHostString(), this.getPort());
    }

    @Override
    public void onStart() {
        this.running = true;
        this.thread.start();
    }

    public void systemBroadcast(@NonNull String message) {
        ChatEvent event = new ChatEvent("-1", message, EventType.getSystemUser(), EventType.getSystemUser());

        for (WebSocket conn : this.getConnections()) {
            SocketClient client = conn.getAttachment();

            client.sendEvent(event);
            client.sendSystemMessage(message);
        }
    }

    public void sendNotices() {
        ClientBannerNotice[] notices = Koi.getInstance().getNotices();

        for (WebSocket conn : this.getConnections()) {
            SocketClient client = conn.getAttachment();

            for (ClientBannerNotice notice : notices) {
                client.sendNotice(notice);
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        SocketClient client = conn.getAttachment();

        client.onClose();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Map<String, List<String>> query = Util.splitQuery(handshake.getResourceDescriptor());
        List<String> clientIdParam = query.get("client_id");
        String clientId = null;

        if ((clientIdParam == null) || clientIdParam.isEmpty()) {
            clientId = "UNKNOWN";
        } else {
            clientId = clientIdParam.get(0);
        }

        if (!this.koi.getClientIds().containsKey(clientId)) {
            clientId = "UNKNOWN";
        }

        SocketClient client = new SocketClient(clientId, conn, this.koi);

        conn.setAttachment(client);

        client.sendWelcomeMessage();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SocketClient client = conn.getAttachment();

        Koi.getClientThreadPool().submit(() -> {
            try {
                JsonObject json = Koi.GSON.fromJson(message, JsonObject.class);
                IncomingMessageType type = GsonEventDeserializer.parseEnumFromJsonElement(IncomingMessageType.values(), json.get("type"));

                if (type == IncomingMessageType.KEEP_ALIVE) {
                    client.onPong();
                } else {
                    AbstractEvent<IncomingMessageType> request = eventDeserializer.deserializeJson(type, json);

                    for (EventWrapper wrapper : client.getWrappers()) {
                        try {
                            wrapper.call(request);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                }
            } catch (JsonParseException e) {
                client.sendError(OutgoingMessageErrorType.REQUEST_JSON_INVAID, null);
            } catch (IllegalArgumentException e) {
                client.sendError(OutgoingMessageErrorType.REQUEST_TYPE_INVAID, null);
            } catch (NullPointerException e) {
                client.sendError(OutgoingMessageErrorType.REQUEST_CRITERIA_INVAID, null);
            } catch (Throwable e) {
                client.sendError(OutgoingMessageErrorType.SERVER_INTERNAL_ERROR, null);
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        FastLogger.logException(e);
    }

}
