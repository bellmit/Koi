package co.casterlabs.koi.user.caffeine;

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
public class CaffeineUserChecker {
    private long lastFollowerCheck = WebUtil.easternTimeCalendar.getTimeInMillis() - (60 * 1000); // minus 60s
    private @NonNull CaffeineUser user;

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
                        String date = follower.get("followed_at").getAsString().replace(".000Z", "");
                        long time = WebUtil.dateFormat.parse(date).getTime();

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

        }
    }

}
