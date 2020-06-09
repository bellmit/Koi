package co.casterlabs.koi.user.caffeine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.AuthProvider;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CaffeineFollowerChecker {
    public static final TimeZone utc = TimeZone.getTimeZone("UTC");
    public static final Calendar utcCalendar = Calendar.getInstance(utc);
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    private long lastFollowerCheck = utcCalendar.getTimeInMillis();
    private @NonNull CaffeineUser user;
    
    static {
        dateFormat.setTimeZone(utc);
    }
    
    public void updateFollowers() {
        try {
            AuthProvider auth = Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE);

            if (this.user.hasListeners() && auth.isLoggedIn()) {
                long highest = this.lastFollowerCheck;

                JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getFollowersLink(this.user.getCAID()), auth.getAuthHeaders(), JsonObject.class);
                JsonArray followers = json.getAsJsonArray("followers");

                if (followers != null) {
                    for (JsonElement je : followers) {
                        JsonObject follower = je.getAsJsonObject();

                        String caid = follower.get("caid").getAsString();
                        String date = follower.get("followed_at").getAsString();
                        long time = dateFormat.parse(date).getTime();

                        if (!this.user.getFollowers().contains(caid) && (time > this.lastFollowerCheck)) {
                            if (highest < time) highest = time;

                            User user = Koi.getInstance().getUser(caid, UserPlatform.CAFFEINE);

                            this.user.broadcastEvent(new FollowEvent(user, this.user));
                        }
                    }

                    this.lastFollowerCheck = highest;
                } // Otherwise random error
            }
        } catch (Exception e) {
            Koi.getInstance().getLogger().exception(e);
        }
    }

}
