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
import co.casterlabs.koi.KoiImpl;
import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.clientid.ClientIdMeta;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.external.Server;
import co.casterlabs.koi.networking.incoming.ChatRequest;
import co.casterlabs.koi.networking.incoming.CredentialsRequest;
import co.casterlabs.koi.networking.incoming.DeleteMyDataRequest;
import co.casterlabs.koi.networking.incoming.DeleteRequest;
import co.casterlabs.koi.networking.incoming.IncomingMessageType;
import co.casterlabs.koi.networking.incoming.PuppetLoginRequest;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UpvoteRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import co.casterlabs.koi.networking.outgoing.ClientBannerNotice;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageErrorType;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageType;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.koi.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.eventapi.events.AbstractEvent;
import xyz.e3ndr.eventapi.events.deserializer.GsonEventDeserializer;
import xyz.e3ndr.eventapi.listeners.EventWrapper;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

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
        eventDeserializer.registerEventClass(IncomingMessageType.DELETE, DeleteRequest.class);
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

                if (client != null) {
                    if (client.isExpired(current)) {
                        client.sendError(OutgoingMessageErrorType.FAILED_KEEP_ALIVE, null);
                        client.onClose();
                        conn.close();
                    } else {
                        client.sendKeepAlive();
                    }
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

        KoiImpl.getInstance().getLogger().info("Koi started on %s:%d!", this.getAddress().getHostString(), this.getPort());
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

    public void systemBroadcast(@NonNull ChatEvent event, @NonNull UserPlatform platform) {
        for (WebSocket conn : this.getConnections()) {
            SocketClient client = conn.getAttachment();

            if ((client.getClient() != null) && client.getClient().getSimpleProfile().getPlatform() == platform) {
                client.sendEvent(event);
                client.sendSystemMessage(event.getMessage());
            }
        }
    }

    public void systemNotice(@NonNull ClientBannerNotice notice, @NonNull UserPlatform platform) {
        for (WebSocket conn : this.getConnections()) {
            SocketClient client = conn.getAttachment();

            if ((client.getClient() != null) && client.getClient().getSimpleProfile().getPlatform() == platform) {
                client.sendNotice(notice);
            }
        }
    }

    public void sendNotices() {
        ClientBannerNotice[] notices = KoiImpl.getInstance().getNotices();

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

        if (client != null) {
            client.onClose();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            Map<String, List<String>> query = Util.splitQuery(handshake.getResourceDescriptor());
            List<String> clientIdParam = query.get("client_id");

            String clientId = null;
            ClientIdMeta meta = null;

            if ((clientIdParam != null) && !clientIdParam.isEmpty()) {
                clientId = clientIdParam.get(0);
                meta = Natsukashii.getClientIdMeta(clientId);
            }

            if (meta == null) {
                clientId = "UNKNOWN";
                meta = ClientIdMeta.UNKNOWN;
            }

            SocketClient client = new SocketClient(meta, clientId, conn, this.koi);

            conn.setAttachment(client);

            client.sendWelcomeMessage();
            client.send(Koi.GSON.toJsonTree(meta).getAsJsonObject(), OutgoingMessageType.CLIENT_SCOPES);
        } catch (Exception e) {
            FastLogger.logException(e);
            conn.close();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SocketClient client = conn.getAttachment();

        try {
            JsonObject json = Koi.GSON.fromJson(message, JsonObject.class);
            IncomingMessageType type = GsonEventDeserializer.parseEnumFromJsonElement(IncomingMessageType.values(), json.get("type"));

            if (type == IncomingMessageType.KEEP_ALIVE) {
                client.onPong();
            } else {
                Koi.clientThreadPool.submit(() -> {
                    try {
                        AbstractEvent<IncomingMessageType> request = eventDeserializer.deserializeJson(type, json);

                        for (EventWrapper wrapper : client.getWrappers()) {
                            try {
                                wrapper.call(request);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        client.sendError(OutgoingMessageErrorType.REQUEST_TYPE_INVAID, null);
                    } catch (NullPointerException e) {
                        FastLogger.logStatic(LogLevel.TRACE, e);
                        client.sendError(OutgoingMessageErrorType.REQUEST_CRITERIA_INVAID, null);
                    } catch (Throwable e) {
                        FastLogger.logStatic("An error occured whilst processing:\n%s", json);
                        FastLogger.logException(e);
                        client.sendError(OutgoingMessageErrorType.SERVER_INTERNAL_ERROR, null);
                    }
                });
            }
        } catch (JsonParseException e) {
            client.sendError(OutgoingMessageErrorType.REQUEST_JSON_INVAID, null);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        FastLogger.logException(e);
    }

}
