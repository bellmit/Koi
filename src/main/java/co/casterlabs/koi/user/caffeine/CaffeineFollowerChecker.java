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
public class CaffeineFollowerChecker {
    private @NonNull CaffeineUser user;
    private boolean isNew = true;

    public void updateFollowers() {
        try {
            AuthProvider auth = Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE);

            if (this.isNew) {
                this.isNew = false;
                
                for (int offset = 0; offset <= this.user.getFollowerCount(); offset += 100) {
                    JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getFollowersLink(this.user.getCAID()) + "&offset=" + offset, auth.getAuthHeaders(), JsonObject.class);
                    JsonArray followers = json.getAsJsonArray("followers");

                    if (followers != null) {
                        for (JsonElement je : followers) {
                            String caid = je.getAsJsonObject().get("caid").getAsString();
                            
                            this.user.getFollowers().add(caid);
                        }
                    }
                }
            } else if (this.user.hasListeners() && auth.isLoggedIn()) {
                JsonObject json = WebUtil.jsonSendHttpGet(CaffeineLinks.getFollowersLink(this.user.getCAID()), auth.getAuthHeaders(), JsonObject.class);
                JsonArray followers = json.getAsJsonArray("followers");

                if (followers != null) {
                    for (JsonElement je : followers) {
                        JsonObject follower = je.getAsJsonObject();

                        String caid = follower.get("caid").getAsString();

                        if (!this.user.getFollowers().contains(caid)) {
                            User user = Koi.getInstance().getUser(caid, UserPlatform.CAFFEINE);

                            this.user.broadcastEvent(new FollowEvent(user, this.user));
                        }
                    }
                } // Otherwise random error
            }
        } catch (Exception e) {
            Koi.getInstance().getLogger().exception(e);
        }
    }

}
