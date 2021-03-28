package co.casterlabs.koi.user.brime;

import java.util.HashSet;

import co.casterlabs.brimeapijava.types.BrimeUser;
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
        asUser.setUUID(user.getUserId());
        asUser.setBadges(new HashSet<>(user.getBadges()));
        // asUser.setRoles(roles); // TODO
        asUser.setImageLink(user.getAvatar());
        asUser.setColor(user.getColor());

        return asUser;
    }

    @Override
    public User get(@NonNull String username) {
        try {
            return this.get(username, null);
        } catch (Exception e) {
            return null;
        }
    }

    public User get(String username, String color) {
        User user = new User(UserPlatform.BRIME);

        user.setUsername(username.toLowerCase());
        user.setDisplayname(username);
        user.setUUID("BRIME_BETA_LEGACY." + username);
        user.setImageLink("https://assets.casterlabs.co/brime/logo.png");

        if (color == null) {
            user.calculateColorFromUsername();
        } else {
            user.setColor(color);
        }

        return user;
    }

}
