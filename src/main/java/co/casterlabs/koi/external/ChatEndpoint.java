package co.casterlabs.koi.external;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
            String message = new String(Base64.getDecoder().decode(session.getHeaders().get("message")), StandardCharsets.UTF_16);
            User user = platform.getUserCache().get(uuid.toUpperCase());

            if (user != null) {
                Koi.getInstance().getAuthProvider(platform).sendChatMessage(user, message);
                return newFixedLengthResponse("true");
            }
        } catch (Exception e) {}

        return newFixedLengthResponse("false");
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
