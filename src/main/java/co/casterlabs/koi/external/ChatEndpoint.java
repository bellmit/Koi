package co.casterlabs.koi.external;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.networking.Server;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import fi.iki.elonen.NanoHTTPD;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class ChatEndpoint extends NanoHTTPD implements Server {

    public ChatEndpoint(int port) throws IOException {
        super(port);

        this.setAsyncRunner(new NanoRunner("Chat Endpoint"));
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            UserPlatform platform = UserPlatform.parse(session.getHeaders().get("platform"));
            String uuid = session.getHeaders().get("uuid");
            String message = getBody(session);
            User user = platform.getUserCache().get(uuid.toUpperCase());

            if (user != null) {
                Koi.getInstance().getAuthProvider(platform).sendChatMessage(user, message);
                return newFixedLengthResponse("true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse("false");
    }

    private static String getBody(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> body = new HashMap<>();

        session.parseBody(body);

        return body.get("postData");
    }

    @SneakyThrows
    @Override
    public void start() {
        super.start();

        FastLogger.logStatic("ChatEndpoint started on port %d!", this.getListeningPort());
    }

    @Override
    public boolean isRunning() {
        return this.isAlive();
    }

}
