package co.casterlabs.koi.user.caffeine;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.requests.CaffeineUser.UserBadge;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

public class CaffeineUserConverter implements UserConverter<co.casterlabs.caffeineapi.requests.CaffeineUser> {
    private static @Getter UserConverter<co.casterlabs.caffeineapi.requests.CaffeineUser> instance = new CaffeineUserConverter();

    @Override
    public @NonNull User transform(@NonNull co.casterlabs.caffeineapi.requests.CaffeineUser user) {
        User result = new User(UserPlatform.CAFFEINE);
        UserBadge badge = user.getBadge();

        if (badge != UserBadge.NONE) {
            result.getBadges().add(badge.getImageLink());
        }

        result.setUUID(user.getCAID());
        result.setUsername(user.getUsername().toLowerCase());
        result.setDisplayname(user.getUsername());
        result.setImageLink(user.getImageLink());

        result.calculateColorFromBio();

        result.getBadges().addAll(Koi.getForcedBadges(UserPlatform.CAFFEINE, user.getCAID()));

        return result;
    }

    @Override
    public @Nullable User get(@NonNull String username) {
        CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

        try {
            return this.transform(request.send());
        } catch (ApiException e) {
            return null;
        }
    }

}
