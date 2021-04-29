package co.casterlabs.koi.external;

import java.nio.charset.StandardCharsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.clientid.ClientIdMeta;
import co.casterlabs.koi.clientid.ClientIdScope;
import co.casterlabs.koi.config.ThirdPartyBannerConfig;
import co.casterlabs.koi.config.ThirdPartyBannerConfig.Banner;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.networking.SocketServer;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class ThirdPartyBroadcastEndpoint extends NanoHTTPD implements Server {

    public ThirdPartyBroadcastEndpoint(int port) {
        super(port);

        this.setAsyncRunner(new NanoRunner("ThirdParty Broadcast"));
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            if (session.getMethod() == Method.POST) {
                byte[] bytes = new byte[session.getInputStream().available()];
                session.getInputStream().read(bytes);

                String body = new String(bytes, StandardCharsets.UTF_8);

                BroadcastRequest request = Koi.GSON.fromJson(body, BroadcastRequest.class);

                ClientIdMeta meta = Natsukashii.verifyClientId(request.clientId, request.secret);

                if (meta != null) {
                    if (request.type == BroadcastType.BANNER) {
                        if (meta.hasScope(ClientIdScope.BROADCAST_BANNER)) {
                            Banner banner = ThirdPartyBannerConfig.addBanner(meta.getBroadcastPlatform(), request.message);

                            SocketServer.getInstance().systemNotice(banner.toNotice(), meta.getBroadcastPlatform());

                            return successResponse(BroadcastType.BANNER, "banner", banner);
                        } else {
                            return errorResponse("MISSING_SCOPE");
                        }
                    } else {
                        if (meta.hasScope(ClientIdScope.BROADCAST_MESSAGE)) {
                            ChatEvent event = new ChatEvent("-1", request.message, meta.getBroadcastPlatform().getPlatformUser(), EventType.getSystemUser());

                            SocketServer.getInstance().systemBroadcast(event, meta.getBroadcastPlatform());

                            return successResponse(BroadcastType.MESSAGE, "event", event);
                        } else {
                            return errorResponse("MISSING_SCOPE");
                        }
                    }
                } else {
                    return errorResponse("VERIFY_FAILED");
                }
            } else {
                return errorResponse("BAD_REQUEST");
            }
        } catch (Exception e) {
            return errorResponse("INTERNAL_ERROR");
        }
    }

    @SneakyThrows
    @Override
    public void start() {
        super.start();

        FastLogger.logStatic("ThirdParty Broadcast Endpoint started on port %d!", this.getListeningPort());
    }

    @Override
    public boolean isRunning() {
        return this.isAlive();
    }

    private static Response errorResponse(String error) {
        JsonObject response = new JsonObject();
        JsonArray errors = new JsonArray();

        errors.add(error);

        response.add("data", JsonNull.INSTANCE);
        response.add("errors", errors);

        return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "application/json", response.toString());
    }

    private static Response successResponse(BroadcastType type, String key, Object obj) {
        JsonObject response = new JsonObject();
        JsonObject data = new JsonObject();
        JsonArray errors = new JsonArray();

        data.add(key, Koi.GSON.toJsonTree(obj));
        data.addProperty("type", type.name());

        response.add("data", data);
        response.add("errors", errors);

        return NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", response.toString());
    }

    private static class BroadcastRequest {
        @SerializedName("client_id")
        private String clientId;
        private String secret;
        private String message;
        private BroadcastType type;

    }

    private static enum BroadcastType {
        MESSAGE,
        BANNER;
    }

}
