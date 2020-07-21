package co.casterlabs.koi.user.caffeine;

import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.koi.status.StatusReporter;
import co.casterlabs.koi.user.User;
import lombok.Getter;
import lombok.Setter;

public class CaffeineStatus implements StatusReporter {
    private static final @Getter String TEST_ACCOUNT = "CAID4201D63D468A46D4B184B369E1374857";
    private static final long ALERT = TimeUnit.MINUTES.toMillis(1);

    private static @Getter CaffeineStatus instance = new CaffeineStatus();

    // private long lastMessage = -1;
    // private long lastQuery = -1;
    private long lastFollow = System.currentTimeMillis();
    private @Setter User account;

    @Override
    public String getName() {
        return "PLATFORM_CAFFEINE";
    }

    @Override
    public void report(JsonObject json) {
        long current = System.currentTimeMillis();
        boolean followersWorking = true;

        if ((this.account != null) && this.account.getFollowers().remove(TEST_ACCOUNT)) {
            this.lastFollow = current;
        } else if ((current - this.lastFollow) >= ALERT) {
            followersWorking = false;
        }

        json.addProperty("followers", followersWorking);
    }

}
