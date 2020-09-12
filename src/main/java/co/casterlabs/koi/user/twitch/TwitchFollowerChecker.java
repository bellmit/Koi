package co.casterlabs.koi.user.twitch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@Getter
@RequiredArgsConstructor
public class TwitchFollowerChecker {
    private @NonNull TwitchUser user;
    private boolean isNew = true;

    public void updateFollowers() {
        try {
            AuthProvider auth = Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);
            JsonObject json = WebUtil.jsonSendHttpGet(TwitchLinks.getUserFollowers(this.user.getUUID()), auth.getAuthHeaders(), JsonObject.class);
            JsonArray followers = json.getAsJsonArray("follows");

            this.user.setFollowerCount(json.get("_total").getAsLong());

            for (JsonElement e : followers) {
                try {
                    JsonObject follow = e.getAsJsonObject();

                    if (follow.get("notifications").getAsBoolean()) {
                        String followerUUID = follow.getAsJsonObject("user").get("_id").getAsString();

                        if (!this.isNew && !this.user.getFollowers().contains(followerUUID)) {
                            SerializedUser serialized = TwitchUserConverter.getInstance().get(followerUUID);
                            FollowEvent event = new FollowEvent(serialized, this.user);

                            this.user.broadcastEvent(event);
                        } else {
                            this.user.getFollowers().add(followerUUID);
                        }
                    }
                } catch (IdentifierException ignore) {}
            }

            this.isNew = false;
        } catch (Exception e) {
            FastLogger.logException(e);
        }
    }

}
