package co.casterlabs.koi.networking;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class SocketServer extends WebSocketServer implements Server {
    public static final long keepAliveInterval = 15000;

    private RepeatingThread thread = new RepeatingThread("Keep Alive - Koi", keepAliveInterval, () -> this.keepAllAlive());
    private @Getter boolean running = false;
    private Koi koi;

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
        String raw = handshake.getFieldValue("slim");
        boolean slim = (raw != null) ? raw.equalsIgnoreCase("true") : false;

        SocketClient client = new SocketClient(handshake.getFieldValue("User-Agent"), slim, conn, this.koi);

        conn.setAttachment(client);

        client.sendServerMessage("Welcome! - Koi v" + Koi.VERSION);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SocketClient client = conn.getAttachment();

        Koi.getClientThreadPool().submit(() -> {
            try {
                JsonObject json = Koi.GSON.fromJson(message, JsonObject.class);
                RequestType type = RequestType.fromString(json.get("request").getAsString());

                switch (type) {
                    case ADD:
                        client.add(json.get("user"), json.get("platform"));
                        break;

                    case CLOSE:
                        conn.close();
                        break;

                    case REMOVE:
                        client.remove(json.get("user"), json.get("platform"));
                        break;

                    case TEST:
                        client.test(json.get("test"));
                        break;

                    case PREFERENCES:
                        client.setPreferences(Koi.GSON.fromJson(json.get("preferences"), ClientPreferences.class));
                        break;

                    case KEEP_ALIVE:
                        break;

                    default:
                        client.sendError(RequestError.REQUEST_JSON_INVAID);
                        break;

                }
            } catch (Exception e) {
                FastLogger.logException(e);
                client.sendError(RequestError.SERVER_INTERNAL_ERROR);
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        FastLogger.logException(e);
    }

}
