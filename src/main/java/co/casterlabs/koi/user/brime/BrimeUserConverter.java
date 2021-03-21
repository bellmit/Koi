package co.casterlabs.koi.user.brime;

import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

public class BrimeUserConverter implements UserConverter<Object> {
    private static final @Getter BrimeUserConverter instance = new BrimeUserConverter();

    @Override
    public @NonNull User transform(@NonNull Object object) {
        // NOT USED
        return null;
    }

    @Override
    public User get(@NonNull String username) {
        return this.get(username, null);
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
