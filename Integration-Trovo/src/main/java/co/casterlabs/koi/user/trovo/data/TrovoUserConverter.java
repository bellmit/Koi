package co.casterlabs.koi.user.trovo.data;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.trovo.TrovoIntegration;
import co.casterlabs.trovoapi.requests.TrovoGetUsersRequest;
import co.casterlabs.trovoapi.requests.data.TrovoUser;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public class TrovoUserConverter implements UserConverter<TrovoUser> {
    private static final @Getter TrovoUserConverter instance = new TrovoUserConverter();
    private static WaterCache cache = new WaterCache();

    static {
        cache.start(TimeUnit.MINUTES, 5);
    }

    @Override
    public @NonNull User transform(@NonNull TrovoUser trovo) {
        User result = new User(UserPlatform.TROVO);

        // Trovo docs say the user id and channel id are the same.
        result.setIdAndChannelId(trovo.getUserId());

        result.setDisplayname(trovo.getNickname());
        result.setUsername(trovo.getUsername());
        result.calculateColorFromUsername();

        return result;
    }

    public User getByNickname(String username) throws IdentifierException {
        TrovoGetUsersRequest request = new TrovoGetUsersRequest(TrovoIntegration.getInstance().getAppAuth(), username);

        try {
            return this.transform(request.send().get(0));
        } catch (ApiException e) {
            throw new IdentifierException();
        }
    }

    @Override
    public User get(@NonNull String username) {
        String key = username.toLowerCase() + ":profile";

        try {
            CachedProfile cached = (CachedProfile) cache.getItemById(key);

            if (cached == null) {
                cached = new CachedProfile(username.toLowerCase());

                cache.registerItem(key, cached);
            }

            cached.wake();

            return cached.user;
        } catch (IdentifierException e) {
            return null;
        }
    }

    private static class CachedProfile extends Cachable {
        private User user;
        private String login;
        private long lastWake;

        public CachedProfile(String login) throws IdentifierException {
            super(TimeUnit.MINUTES, 15);

            this.login = login;
            this.user = TrovoUserConverter.getInstance().getByNickname(login);
        }

        public void wake() {
            this.lastWake = System.currentTimeMillis();
        }

        @Override
        public boolean onDispose(DisposeReason reason) {
            if ((System.currentTimeMillis() - this.lastWake) > TimeUnit.MINUTES.toMillis(15)) {
                this.life += TimeUnit.MINUTES.toMillis(15);

                try {
                    this.user = TrovoUserConverter.getInstance().getByNickname(this.login);
                } catch (IdentifierException ignored) {}

                return false;
            } else {
                return true;
            }
        }

    }

}
