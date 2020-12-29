package co.casterlabs.koi.user.caffeine;

import co.casterlabs.caffeineapi.requests.CaffeineUser.UserBadge;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;

// TODO 
public class CaffeineUserConverter implements UserConverter<co.casterlabs.caffeineapi.requests.CaffeineUser> {
    private static @Getter UserConverter<co.casterlabs.caffeineapi.requests.CaffeineUser> instance = new CaffeineUserConverter();

    @Override
    public SerializedUser transform(co.casterlabs.caffeineapi.requests.CaffeineUser user) {
        SerializedUser result = new SerializedUser(UserPlatform.CAFFEINE);
        UserBadge badge = user.getBadge();

        // result.getBadges().addAll(preferences.getForcedBadges());

        // TODO parse bio for color.

        if (badge != UserBadge.NONE) {
            result.getBadges().add(badge.getImageLink());
        }

        result.setUUID(user.getCAID());
        result.setUsername(user.getUsername());
        result.setImageLink(user.getImageLink());
        // result.setColor(preferences.get(PolyFillRequirements.COLOR));

        return result;
    }

}
