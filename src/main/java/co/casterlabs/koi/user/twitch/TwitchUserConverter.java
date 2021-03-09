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
import co.casterlabs.twitchapi.helix.requests.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.types.HelixUser;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter TwitchUserConverter instance = new TwitchUserConverter();
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 5);
    }

    @Override
    public @NonNull User transform(@NonNull com.gikk.twirk.types.users.TwitchUser user) {
        User result = new User(UserPlatform.TWITCH);

        String color = "#" + Integer.toHexString(user.getColor()).toUpperCase();
        String id = String.valueOf(user.getUserID());

        setColor(id, color);

        result.setUUID(id);
        result.setUsername(user.getUserName());
        result.setColor(color);
        result.setImageLink(getProfilePicture(user.getUserName()));
        result.setDisplayname(user.getDisplayName().isEmpty() ? user.getUserName() : user.getDisplayName());

        return result;
    }

    private User getByLogin(String login) throws IdentifierException {
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

    public User getByID(String id) throws IdentifierException {
        HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        request.addId(id);

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

        result.setUsername(helix.getLogin());
        result.setUUID(helix.getId());
        result.setImageLink(helix.getProfileImageUrl());
        result.setDisplayname(helix.getDisplayName().isEmpty() ? helix.getLogin() : helix.getDisplayName());
        result.setColor(getColor(helix.getId()));

        return result;
    }

    @Override
    public @Nullable User get(@NonNull String username) {
        String key = username.toLowerCase() + ":profile";

        try {
            CachedProfile cached = (CachedProfile) cache.getItemById(key);

            if (cached == null) {
                cached = new CachedProfile(username);

                cache.registerItem(key, cached);
            }

            cached.wake();

            return cached.user;
        } catch (IdentifierException e) {
            return null;
        }
    }

    private static String getProfilePicture(String login) {
        String key = login + ":image";

        CachedProfilePicture cached = (CachedProfilePicture) cache.getItemById(key);

        if (cached == null) {
            cached = new CachedProfilePicture(login);

            cache.registerItem(key, cached);
        }

        cached.wake();

        return cached.image;
    }

    public static String getColor(String id) {
        String key = id + ":color";

        CachedColor cached = (CachedColor) cache.getItemById(key);

        if (cached == null) {
            cached = new CachedColor();

            cache.registerItem(key, cached);
        }

        cached.wake();

        return cached.color;
    }

    public static void setColor(String id, String color) {
        String key = id + ":color";

        CachedColor cached = (CachedColor) cache.getItemById(key);

        if (cached == null) {
            cached = new CachedColor();

            cache.registerItem(key, cached);
        }

        cached.color = color;
        cached.wake();
    }

    private static class CachedProfile extends Cachable {
        private User user;
        private String login;
        private long lastWake;

        public CachedProfile(String login) throws IdentifierException {
            super(TimeUnit.MINUTES, 15);

            this.login = login;
            this.user = TwitchUserConverter.getInstance().getByLogin(login);
        }

        public void wake() {
            this.lastWake = System.currentTimeMillis();
        }

        @Override
        public boolean onDispose(DisposeReason reason) {
            if ((System.currentTimeMillis() - this.lastWake) > TimeUnit.MINUTES.toMillis(15)) {
                this.life += TimeUnit.MINUTES.toMillis(15);

                try {
                    this.user = TwitchUserConverter.getInstance().getByLogin(login);
                } catch (IdentifierException ignored) {}

                return false;
            } else {
                return true;
            }
        }

    }

    private static class CachedColor extends Cachable {
        private String color = "#FFFFFF";
        private long lastWake;

        public CachedColor() {
            super(TimeUnit.HOURS, 1);
        }

        public void wake() {
            this.lastWake = System.currentTimeMillis();
        }

        @Override
        public boolean onDispose(DisposeReason reason) {
            if ((System.currentTimeMillis() - this.lastWake) > TimeUnit.MINUTES.toMillis(15)) {
                this.life += TimeUnit.HOURS.toMillis(1);

                return false;
            } else {
                return true;
            }
        }

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
