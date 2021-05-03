package co.casterlabs.koi.integration.brime.user;

import java.util.HashSet;

import co.casterlabs.brimeapijava.requests.BrimeGetUserRequest;
import co.casterlabs.brimeapijava.types.BrimeUser;
import co.casterlabs.koi.integration.brime.BrimeIntegration;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

public class BrimeUserConverter implements UserConverter<BrimeUser> {
    private static final @Getter BrimeUserConverter instance = new BrimeUserConverter();

    @Override
    public @NonNull User transform(@NonNull BrimeUser user) {
        User asUser = new User(UserPlatform.BRIME);

        asUser.setDisplayname(user.getDisplayname());
        asUser.setUsername(user.getUsername());
        asUser.setId(user.getUserId());
        asUser.setBadges(new HashSet<>(user.getBadges()));
        // asUser.setRoles(roles); // TODO
        asUser.setImageLink(user.getAvatar());
        asUser.setColor(user.getColor());

        return asUser;
    }

    @Override
    public User get(@NonNull String username) {
        try {
            BrimeUser user = new BrimeGetUserRequest(BrimeIntegration.getInstance().getAppAuth()).setName(username).send();

            return this.transform(user);
        } catch (Exception e) {
            return null;
        }
    }

}
