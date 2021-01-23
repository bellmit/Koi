package co.casterlabs.koi.external;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.ErrorReporting;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.networking.Server;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.twitch.TwitchCredentialsAuth;
import co.casterlabs.twitchapi.ThreadHelper;
import co.casterlabs.twitchapi.TwitchApi;
import co.casterlabs.twitchapi.helix.HelixGetStreamsRequest.HelixStream;
import co.casterlabs.twitchapi.helix.HelixGetUserFollowersRequest;
import co.casterlabs.twitchapi.helix.HelixGetUserFollowersRequest.HelixFollower;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest.HelixUser;
import co.casterlabs.twitchapi.helix.webhooks.HelixGetWebhookSubscriptionsRequest;
import co.casterlabs.twitchapi.helix.webhooks.HelixGetWebhookSubscriptionsRequest.WebhookSubscription;
import co.casterlabs.twitchapi.helix.webhooks.HelixWebhookSubscribeRequest;
import co.casterlabs.twitchapi.helix.webhooks.WebhookUtil;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

//This is poorly written, but we need it to be *functional* so...
public class TwitchWebhookEndpoint extends NanoHTTPD implements Server {
    private static @Getter TwitchWebhookEndpoint instance;

    private String secret = Integer.toHexString(ThreadLocalRandom.current().nextInt(1000, Integer.MAX_VALUE));
    private Map<String, Consumer<JsonObject>> callbacks = new HashMap<>();
    private String url;

    public TwitchWebhookEndpoint(String url, int port) {
        super(port);

        this.url = url;

        instance = this;

        this.setAsyncRunner(new NanoRunner("Twitch Webhook"));
    }

    @SneakyThrows
    @Override
    public void stop() {
        TwitchCredentialsAuth auth = (TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);
        HelixGetWebhookSubscriptionsRequest request = new HelixGetWebhookSubscriptionsRequest(auth);

        for (WebhookSubscription sub : request.send()) {
            sub.remove(auth);
        }

        ThreadHelper.executeAsyncLater("Webhook stop thread", () -> super.stop(), TimeUnit.MINUTES.toMillis(1)); // Close ALL webhooks
    }

    @SuppressWarnings("deprecation")
    @Override
    public Response serve(IHTTPSession session) {
        try {
            Map<String, String> parameters = session.getParms();
            String mode = parameters.getOrDefault("hub.mode", "");

            if (mode.equals("unsubscribe")) {
                this.callbacks.remove(parameters.get("hub.topic"));
                return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, parameters.get("hub.challenge"));
            } else if (mode.equals("subscribe")) {
                return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, parameters.get("hub.challenge"));
            } else {
                byte[] bytes = new byte[session.getInputStream().available()];
                session.getInputStream().read(bytes);

                String signature = session.getHeaders().get("x-hub-signature");
                String body = new String(bytes, StandardCharsets.UTF_8);

                if ((signature != null) && (this.secret != null)) {
                    String sha256 = signature.split("=")[1];
                    String result = WebhookUtil.createSignatureWithSHA256(this.secret, body);

                    if (!result.equalsIgnoreCase(sha256)) {
                        return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "");
                    }
                }

                try {
                    // We ignore checks, as we have a catch all.
                    JsonObject json = TwitchApi.GSON.fromJson(body, JsonObject.class);
                    String topic = parameters.get("hub.topic");
                    Consumer<JsonObject> callback = this.callbacks.get(topic);

                    if (callback != null) {
                        callback.accept(json);
                    }
                } catch (Exception e) {
                    ErrorReporting.webhookerror(session.getUri(), body, session.getHeaders(), e);
                }
            }
        } catch (Exception e) {
            ErrorReporting.uncaughterror(e);
        }

        return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }

    @SuppressWarnings("deprecation")
    public HelixWebhookSubscribeRequest addHook(@NonNull String topic, @NonNull Consumer<JsonObject> callback) throws ApiException, ApiAuthException, IOException {
        HelixWebhookSubscribeRequest request = new HelixWebhookSubscribeRequest(HelixWebhookSubscribeRequest.WebhookSubscribeMode.SUBSCRIBE, this.url + "?hub.topic=" + URLEncoder.encode(topic), topic, (TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        this.callbacks.put(topic, callback);

        request.setLease(1, TimeUnit.HOURS);
        request.setAutoRefresh(true);
        request.setSecret(this.secret);
        request.send();

        return request;
    }

    public HelixWebhookSubscribeRequest addFollowerHook(@NonNull String id, @NonNull Consumer<HelixFollower> callback) throws ApiException, ApiAuthException, IOException {
        return this.addHook("https://api.twitch.tv/helix/users/follows?first=1&to_id=" + id, (json) -> {
            JsonObject data = json.getAsJsonArray("data").get(0).getAsJsonObject();
            HelixGetUserFollowersRequest.HelixFollower follower = new HelixFollower(data.get("from_id").getAsString(), Instant.now());

            callback.accept(follower);
        });
    }

    public HelixWebhookSubscribeRequest addStreamHook(@NonNull String id, @NonNull Consumer<HelixStream> callback) throws ApiException, ApiAuthException, IOException {
        return this.addHook("https://api.twitch.tv/helix/streams?user_id=" + id, (json) -> {
            JsonArray data = json.getAsJsonArray("data");

            if (data.size() == 0) {
                callback.accept(null);
            } else {
                HelixStream stream = TwitchApi.GSON.fromJson(data.get(0), HelixStream.class);

                callback.accept(stream);
            }
        });
    }

    public HelixWebhookSubscribeRequest addUserProfileHook(@NonNull String id, @NonNull Consumer<HelixUser> callback) throws ApiException, ApiAuthException, IOException {
        return this.addHook("https://api.twitch.tv/helix/users?id=" + id, (json) -> {
            JsonArray data = json.getAsJsonArray("data");
            HelixGetUsersRequest.HelixUser profile = TwitchApi.GSON.fromJson(data.get(0), HelixUser.class);

            callback.accept(profile);
        });
    }

    @SneakyThrows
    @Override
    public void start() {
        super.start();

        TwitchCredentialsAuth auth = (TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);
        HelixGetWebhookSubscriptionsRequest request = new HelixGetWebhookSubscriptionsRequest(auth);

        for (WebhookSubscription sub : request.send()) {
            sub.remove(auth);
        }

        FastLogger.logStatic("TwitchWebhookEndpoint started on port %d!", this.getListeningPort());
    }

    @Override
    public boolean isRunning() {
        return this.isAlive();
    }

}
