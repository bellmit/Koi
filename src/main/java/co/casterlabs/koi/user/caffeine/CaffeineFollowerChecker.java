package co.casterlabs.koi.user.caffeine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@Getter
@RequiredArgsConstructor
public class CaffeineFollowerChecker {
    private @NonNull CaffeineUser user;
    private boolean isNew = true;

    public void updateFollowers() {
        JsonObject json = null;

        try {
            AuthProvider auth = Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE);

            if (this.isNew) {
                this.isNew = false;

                for (int offset = 0; offset <= this.user.getFollowerCount(); offset += 100) {
                    json = WebUtil.jsonSendHttpGet(CaffeineLinks.getFollowersLink(this.user.getUUID()) + "&offset=" + offset, auth.getAuthHeaders(), JsonObject.class);
                    JsonArray followers = json.getAsJsonArray("followers");

                    for (JsonElement je : followers) {
                        String caid = je.getAsJsonObject().get("caid").getAsString();

                        this.user.getFollowers().add(caid);
                    }
                }
            } else if (this.user.hasListeners() && auth.isLoggedIn()) {
                json = WebUtil.jsonSendHttpGet(CaffeineLinks.getFollowersLink(this.user.getUUID()), auth.getAuthHeaders(), JsonObject.class);
                JsonArray followers = json.getAsJsonArray("followers");

                for (JsonElement je : followers) {
                    JsonObject follower = je.getAsJsonObject();
                    String caid = follower.get("caid").getAsString();

                    if (!this.user.getFollowers().contains(caid)) {
                        SerializedUser user = Koi.getInstance().getUserSerialized(caid, UserPlatform.CAFFEINE);

                        this.user.broadcastEvent(new FollowEvent(user, this.user));
                    }

                }
            }
        } catch (Exception e) {
            FastLogger.logStatic("An error occured with the following payload:\n%s", json);
            FastLogger.logException(e);
        }
    }

}
