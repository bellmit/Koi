package co.casterlabs.koi.user.caffeine;

import java.util.List;

import co.casterlabs.caffeineapi.CaffeineAuth;
import co.casterlabs.caffeineapi.requests.CaffeineFollowersListRequest;
import co.casterlabs.caffeineapi.requests.CaffeineFollowersListRequest.CaffeineFollower;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@Getter
public class CaffeineFollowerChecker {
    private @NonNull CaffeineUser user;

    public CaffeineFollowerChecker(CaffeineUser user) {
        this.user = user;

        CaffeineFollowersListRequest request = new CaffeineFollowersListRequest((CaffeineAuth) Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE));

        request.setCAID(this.user.getUUID());
        request.sendAsync().thenAccept((followers) -> {
            for (CaffeineFollower follower : followers) {
                this.user.getFollowers().add(follower.getCAID());
            }
        });
    }

    public void updateFollowers() {
        try {
            CaffeineFollowersListRequest request = new CaffeineFollowersListRequest((CaffeineAuth) Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE));

            request.setCAID(this.user.getUUID());

            List<CaffeineFollower> followers = request.send();

            for (CaffeineFollower follower : followers) {
                if (this.user.getFollowers().add(follower.getCAID())) {
                    SerializedUser user = CaffeineUserConverter.getInstance().get(follower.getCAID());

                    this.user.broadcastEvent(new FollowEvent(user, this.user));
                }
            }
        } catch (Exception e) {
            FastLogger.logException(e);
        }
    }

}
