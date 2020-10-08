package co.casterlabs.koi.networking;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private @Getter Map<WebSocket, SocketClient> configs = new ConcurrentHashMap<>();
    private @Getter boolean running = false;
    private Koi koi;

    public SocketServer(InetSocketAddress bind, Koi koi) {
        super(bind);

        this.koi = koi;
    }

    private void keepAllAlive() {
        if (this.running) {
            for (SocketClient client : this.configs.values()) {
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
        this.configs.get(conn).close();
        this.configs.remove(conn);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        SocketClient config = new SocketClient(handshake.getFieldValue("User-Agent"), conn, this.koi);

        this.configs.put(conn, config);

        config.sendServerMessage("Welcome! - Koi v" + Koi.VERSION);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        SocketClient config = this.configs.get(conn);

        config.getThreadPool().submit(() -> {
            try {
                JsonObject json = Koi.GSON.fromJson(message, JsonObject.class);
                RequestType type = RequestType.fromString(json.get("request").getAsString());

                switch (type) {
                    case ADD:
                        config.add(json.get("user"), json.get("platform"));
                        break;

                    case CLOSE:
                        conn.close();
                        break;

                    case REMOVE:
                        config.remove(json.get("user"), json.get("platform"));
                        break;

                    case TEST:
                        config.test(json.get("user"), json.get("platform"), json.get("test"));
                        break;

                    case PREFERENCES:
                        config.setPreferences(Koi.GSON.fromJson(json.get("preferences"), ClientPreferences.class));
                        break;

                    case KEEP_ALIVE:
                        break;

                    default:
                        config.sendError(RequestError.REQUEST_JSON_INVAID);
                        break;

                }
            } catch (Exception e) {
                FastLogger.logException(e);
                config.sendError(RequestError.SERVER_INTERNAL_ERROR);
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        FastLogger.logException(e);
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> queryPairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            int index = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"), URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
        }

        return queryPairs;
    }

}
