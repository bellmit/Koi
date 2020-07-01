package co.casterlabs.koi.user.caffeine;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.ShareEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.SneakyThrows;

public class CaffeineMessages extends WebSocketClient {
    private static final String loggedInMessage = "{\"Body\":\"\",\"Compatibility-Mode\":false,\"Headers\":{\"Content-Type\":\"application/x-json-event-stream\"},\"Status\":200}\n"; // I hate caffeine for the rando newlines.
    private static final String loginHeader = "{\"Headers\":{\"Authorization\":\"Anonymous Fish\",\"X-Client-Type\":\"api\"}}";
    private static final long caffeineKeepAlive = 15000;

    private KeepAlive keepAlive = new KeepAlive();
    private CaffeineUser user;

    @SneakyThrows
    public CaffeineMessages(CaffeineUser user) {
        super(CaffeineLinks.getMessagesLink(user.getStageId()));

        this.setProxy(WebUtil.getProxy());
        this.user = user;
        this.connect();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.send(loginHeader);
        this.keepAlive.start();
    }

    @Override
    public void onMessage(String message) {
        try {
            if (!message.equals("\"THANKS\"") && !message.equals(loggedInMessage)) {
                JsonObject json = WebUtil.getJsonFromString(message, JsonObject.class);
                JsonObject publisher = json.getAsJsonObject("publisher");
                JsonObject body = json.getAsJsonObject("body");
                CaffeineAlertType type = CaffeineAlertType.valueOfString(json.get("type").getAsString().toUpperCase());

                // Supporting upvotes would break cross platform compatibility.
                if (!json.has("endorsement_count") && (type != null)) {
                    User sender = Koi.getInstance().getUser(publisher.get("caid").getAsString(), UserPlatform.CAFFEINE);

                    switch (type) {
                        case SHARE:
                            this.user.broadcastEvent(new ShareEvent(body.get("text").getAsString(), sender, this.user));
                            break;

                        case REACTION:
                            // this.benchmark(body, sender);
                            this.user.broadcastEvent(new ChatEvent(body.get("text").getAsString(), sender, this.user));
                            break;

                        case DIGITAL_ITEM:
                            JsonObject donation = body.getAsJsonObject("digital_item");
                            String image = CaffeineLinks.getImageLink(donation.get("static_image_path").getAsString());
                            int amount = donation.get("count").getAsInt() * donation.get("credits_per_item").getAsInt();

                            this.user.broadcastEvent(new DonationEvent(body.get("text").getAsString(), sender, this.user, image, "DIGIES", amount));
                            break;

                        case UNKNOWN:
                            Koi.getInstance().getLogger().debug(json.toString());
                            break;
                    }
                }
            }
        } catch (Exception e) {
            Koi.getInstance().getLogger().exception(e); // Prevents the socket from closing.
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(reason);
        
        if (this.user.hasListeners()) {
            Koi.getMiscThreadPool().submit(() -> this.reconnect());
        }
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    private class KeepAlive extends Thread {
        @SneakyThrows
        @Override
        public void run() {
            while (isOpen()) {
                if (!user.hasListeners()) {
                    closeBlocking();
                    return;
                } else {
                    send("\"HEALZ\"");
                    Thread.sleep(caffeineKeepAlive);
                }
            }
        }
    }

}
