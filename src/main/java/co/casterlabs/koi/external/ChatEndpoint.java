package co.casterlabs.koi.external;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import fi.iki.elonen.NanoHTTPD;

public class ChatEndpoint extends NanoHTTPD {

    public ChatEndpoint(int port) throws IOException {
        super(port);
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

}
