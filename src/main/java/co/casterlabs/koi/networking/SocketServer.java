package co.casterlabs.koi.networking;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.networking.incoming.RequestType;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.eventapi.events.AbstractEvent;
import xyz.e3ndr.eventapi.events.deserializer.GsonEventDeserializer;
import xyz.e3ndr.eventapi.listeners.EventWrapper;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class SocketServer extends WebSocketServer implements Server {
    public static final long keepAliveInterval = 15000;

    private static GsonEventDeserializer<RequestType> eventDeserializer = new GsonEventDeserializer<>();

    private RepeatingThread thread = new RepeatingThread("Keep Alive - Koi", keepAliveInterval, () -> this.keepAllAlive());
    private @Getter boolean running = false;
    private Koi koi;

    static {
        eventDeserializer.registerEventClass(RequestType.TEST, TestEventRequest.class);
        eventDeserializer.registerEventClass(RequestType.USER_STREAM_STATUS, UserStreamStatusRequest.class);
        eventDeserializer.registerEventClass(RequestType.LOGIN, UserLoginRequest.class);
    }

    public SocketServer(InetSocketAddress bind, Koi koi) {
        super(bind);

        this.koi = koi;
    }

    private void keepAllAlive() {
        if (this.running) {
            for (WebSocket conn : this.getConnections()) {
                SocketClient client = conn.getAttachment();

                client.sendKeepAlive();
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

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        SocketClient client = conn.getAttachment();

        client.close();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        SocketClient client = new SocketClient(handshake.getFieldValue("User-Agent"), conn, this.koi);

        conn.setAttachment(client);

        client.sendServerMessage("Welcome! - Koi v" + Koi.VERSION);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SocketClient client = conn.getAttachment();

        Koi.getClientThreadPool().submit(() -> {
            try {
                JsonObject json = Koi.GSON.fromJson(message, JsonObject.class);
                RequestType type = (RequestType) GsonEventDeserializer.parseEnumFromJsonElement(RequestType.values(), json.get("type"));

                if (type != RequestType.KEEP_ALIVE) {
                    AbstractEvent<RequestType> request = eventDeserializer.deserializeJson(type, json);

                    for (EventWrapper wrapper : client.getWrappers()) {
                        wrapper.call(request);
                    }
                }
            } catch (JsonParseException e) {
                client.sendError(RequestError.REQUEST_JSON_INVAID);
            } catch (IllegalArgumentException e) {
                client.sendError(RequestError.REQUEST_TYPE_INVAID);
            } catch (NullPointerException e) {
                client.sendError(RequestError.REQUEST_CRITERIA_INVAID);
            } catch (Exception e) {
                client.sendError(RequestError.SERVER_INTERNAL_ERROR);
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        FastLogger.logException(e);
    }

}
