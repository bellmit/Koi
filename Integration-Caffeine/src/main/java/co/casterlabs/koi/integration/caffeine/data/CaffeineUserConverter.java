package co.casterlabs.koi.integration.caffeine.data;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.caffeineapi.types.CaffeineUser;
import co.casterlabs.caffeineapi.types.UserBadge;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

public class CaffeineUserConverter implements UserConverter<CaffeineUser> {
    private static @Getter UserConverter<CaffeineUser> instance = new CaffeineUserConverter();

    @Override
    public @NonNull User transform(@NonNull CaffeineUser user) {
        User result = new User(UserPlatform.CAFFEINE);
        UserBadge badge = user.getBadge();

        if (badge != UserBadge.NONE) {
            result.getBadges().add(badge.getImageLink());
        }

        for (String statusBadge : user.getStatusBadges()) {
            result.getBadges().add(statusBadge);
        }

        result.setIdAndChannelId(user.getCAID());
        result.setUsername(user.getUsername().toLowerCase());
        result.setDisplayname(user.getUsername());
        result.setImageLink(user.getImageLink());

        result.calculateColorFromBio();

        return result;
    }

    @Override
    public @Nullable User get(@NonNull String username) {
        CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

        request.setUsername(username);

        try {
            return this.transform(request.send());
        } catch (ApiException e) {
            return null;
        }
    }

}
