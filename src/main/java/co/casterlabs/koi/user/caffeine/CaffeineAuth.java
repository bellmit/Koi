package co.casterlabs.koi.user.caffeine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.EventListener;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
public class CaffeineAuth implements AuthProvider {
    private static FastLogger logger = new FastLogger();

    private String refreshToken;
    private String signedCredential;
    private String accessToken;
    private String credential;
    private String caid;
    private boolean isNew = true;

    private RepeatingThread thread = new RepeatingThread(TimeUnit.MINUTES.toMillis(10), () -> {
        try {
            this.refresh();

            if (this.isNew) {
                this.isNew = false;

                User user = Koi.getInstance().getUser("Casterlabs", UserPlatform.CAFFEINE);

                CaffeineStatus.getInstance().setAccount(user);

                user.tryExternalHook();
                user.getEventListeners().add(new EventListener() {
                    @Override
                    public void onEvent(Event e) {}
                });
            }
        } catch (Exception e) {
            logger.severe("Caffeine login unsuccessful, unknown error");
            logger.exception(e);

            this.stop();
        }
    });

    public CaffeineAuth(String refreshToken) {
        this.refreshToken = refreshToken;

        if (this.refreshToken != null) {
            this.thread.start();
        }
    }

    private void stop() {
        this.thread.stop();
    }

    @Override
    public boolean isLoggedIn() {
        return (this.accessToken != null) && (this.credential != null) && (this.signedCredential != null) && (this.caid != null);
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        HashMap<String, String> ret = new HashMap<>();

        ret.put("Authorization", "Bearer " + this.accessToken);

        return ret;
    }

    @Override
    public UserPlatform getPlatform() {
        return UserPlatform.CAFFEINE;
    }

    @Override
    public void refresh() throws Exception {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("refresh_token", this.refreshToken);

            JsonObject token = WebUtil.jsonSendHttp(request.toString(), CaffeineLinks.getTokenLink(), Collections.singletonMap("Content-Type", "application/json"), JsonObject.class);

            this.accessToken = token.get("access_token").getAsString();
            this.caid = token.get("caid").getAsString();
            this.credential = token.get("credentials").getAsJsonObject().get("credential").getAsString();

            JsonObject signed = WebUtil.jsonSendHttpGet(CaffeineLinks.getTokenSigningLink(this.caid), getAuthHeaders(), JsonObject.class);

            if (!signed.has("errors")) {
                this.signedCredential = signed.get("token").getAsString();

                logger.debug("Caffeine login successful! Logged in as %s", this.caid);
            } else {
                logger.severe("Caffeine login unsuccessful, reply: \n%s", signed.toString());
            }
        } catch (Exception e) {
            FastLogger.logStatic("Exception whilst logging in:");
            FastLogger.logException(e);
            throw e;
        }
    }

}
