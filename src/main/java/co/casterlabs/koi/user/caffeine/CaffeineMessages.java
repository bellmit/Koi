package co.casterlabs.koi.user.caffeine;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.ShareEvent;
import co.casterlabs.koi.events.UpvoteEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.SneakyThrows;

public class CaffeineMessages extends WebSocketClient {
    private static final String LOGIN_HEADER = "{\"Headers\":{\"Authorization\":\"Anonymous Fish\",\"X-Client-Type\":\"api\"}}";
    private static final long CAFFEINE_KEEPALIVE = TimeUnit.SECONDS.toMillis(15);

    private CaffeineUser user;
    private RepeatingThread keepAlive = new RepeatingThread(CAFFEINE_KEEPALIVE, () -> {
        try {
            if (!this.isOpen()) {
                this.keepAlive.stop();
            } else if (this.user.hasListeners()) {
                this.send("\"HEALZ\"");
                Thread.sleep(CAFFEINE_KEEPALIVE);
            } else {
                this.closeBlocking();
                this.keepAlive.stop();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });

    @SneakyThrows
    public CaffeineMessages(CaffeineUser user) {
        super(CaffeineLinks.getMessagesLink(user.getStageId()));

        this.setProxy(WebUtil.getProxy());
        this.user = user;
        this.connect();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.send(LOGIN_HEADER);
        this.keepAlive.start();
    }

    @Override
    public void onMessage(String message) {
        try {
            if (!message.equals("\"THANKS\"")) {
                JsonObject json = WebUtil.getJsonFromString(message, JsonObject.class);

                if (!json.has("Compatibility-Mode")) {
                    JsonObject publisher = json.getAsJsonObject("publisher");
                    JsonObject body = json.getAsJsonObject("body");
                    CaffeineAlertType type = CaffeineAlertType.valueOfString(json.get("type").getAsString().toUpperCase());

                    if (type != null) {
                        User sender = Koi.getInstance().getUser(publisher.get("caid").getAsString(), UserPlatform.CAFFEINE, publisher);
                        Event event = null;
                        String id = getId(json.get("id").getAsString());

                        switch (type) {
                            case SHARE:
                                event = new ShareEvent(id, body.get("text").getAsString(), sender, this.user);
                                break;

                            case REACTION:
                                event = new ChatEvent(id, body.get("text").getAsString(), sender, this.user);
                                break;

                            case DIGITAL_ITEM:
                                JsonObject donation = body.getAsJsonObject("digital_item");
                                String image = CaffeineLinks.getImageLink(donation.get("static_image_path").getAsString());
                                int amount = donation.get("count").getAsInt() * donation.get("credits_per_item").getAsInt();

                                event = new DonationEvent(id, body.get("text").getAsString(), sender, this.user, image, "DIGIES", amount);
                                break;

                            case UNKNOWN:
                                Koi.getInstance().getLogger().debug(json.toString());
                                return;
                        }

                        if (json.has("endorsement_count")) {
                            this.user.broadcastEvent(new UpvoteEvent(event, json.get("endorsement_count").getAsInt()));
                        } else {
                            this.user.broadcastEvent(event);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Koi.getInstance().getLogger().exception(e); // Prevents the socket from closing.
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.keepAlive.stop();

        if (this.user.hasListeners()) {
            Koi.getMiscThreadPool().submit(() -> this.reconnect());
        }
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    private static String getId(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64);
        JsonObject json = Koi.GSON.fromJson(new String(bytes), JsonObject.class);

        return json.get("u").getAsString();
    }

}
