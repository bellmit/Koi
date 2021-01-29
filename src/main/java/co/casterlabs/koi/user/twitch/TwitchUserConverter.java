package co.casterlabs.koi.user.twitch;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest.HelixUser;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter TwitchUserConverter instance = new TwitchUserConverter();
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 1);
    }

    @Override
    public @NonNull User transform(@NonNull com.gikk.twirk.types.users.TwitchUser user) {
        User result = new User(UserPlatform.TWITCH);

        result.setUUID(String.valueOf(user.getUserID()));
        result.setUsername(user.getDisplayName().isEmpty() ? user.getUserName() : user.getDisplayName());
        result.setColor("#" + Integer.toHexString(user.getColor()).toUpperCase());
        result.setImageLink(getProfilePicture(user.getUserName()));

        result.getBadges().addAll(Koi.getForcedBadges(UserPlatform.TWITCH, String.valueOf(user.getUserID())));

        return result;
    }

    public User getByLogin(String login) throws IdentifierException {
        HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        request.addLogin(login.toLowerCase());

        try {
            List<HelixUser> users = request.send();

            if (!users.isEmpty()) {
                return transform(users.get(0));
            } else {
                throw new IdentifierException();
            }
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    public static User transform(HelixUser helix) {
        User result = new User(UserPlatform.TWITCH);

        result.setUsername(helix.getDisplayName()); // Intentional.
        result.setUUID(helix.getId());
        result.setImageLink(helix.getProfileImageUrl());

        return result;
    }

    @Override
    public @Nullable User get(@NonNull String username) {
        try {
            return this.getByLogin(username);
        } catch (IdentifierException e) {
            return null;
        }
    }

    private static String getProfilePicture(String login) {
        CachedProfilePicture cached = (CachedProfilePicture) cache.getItemById(login);

        if (cached == null) {
            cached = new CachedProfilePicture(login);

            cache.registerItem(login, cached);
        }

        cached.wake();

        return cached.image;
    }

    private static class CachedProfilePicture extends Cachable {
        private String login;

        private String image;
        private long lastWake;

        public CachedProfilePicture(String login) {
            super(TimeUnit.MINUTES, 30);

            this.login = login;

            try {
                User user = instance.getByLogin(this.login);

                this.image = user.getImageLink();
            } catch (IdentifierException e) {}
        }

        public void wake() {
            this.lastWake = System.currentTimeMillis();
        }

        @Override
        public boolean onDispose(DisposeReason reason) {
            if ((System.currentTimeMillis() - this.lastWake) > TimeUnit.MINUTES.toMillis(15)) {
                this.life += TimeUnit.MINUTES.toMillis(30);

                try {
                    User user = instance.getByLogin(this.login);

                    this.image = user.getImageLink();

                    return false;
                } catch (IdentifierException e) {
                    return true;
                }
            } else {
                return true;
            }
        }

    }

}
