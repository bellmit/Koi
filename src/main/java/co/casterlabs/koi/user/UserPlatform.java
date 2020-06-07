package co.casterlabs.koi.user;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import co.casterlabs.koi.IdentifierException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.user.caffeine.CaffeineUser;
import lombok.Getter;
import lombok.SneakyThrows;

public enum UserPlatform {
    CAFFEINE,
    TWITCH,
    MIXER,
    YOUTUBE,
    CASTERLABS;

    private static final long REMOVE_AGE = TimeUnit.MINUTES.toMillis(5);
    private static final long REPEAT = 5000; // 5s

    private @Getter Map<String, User> userCache = new ConcurrentHashMap<>();
    private @Getter File userDir = new File(Koi.getInstance().getDir(), "/users/" + this + "/");

    private RepeatingThread checkThread = new RepeatingThread(REPEAT, () -> {
        long current = System.currentTimeMillis();

        for (User user : this.userCache.values()) {
            if (user.hasListeners()) {
                user.update();
                user.wake();
            } else if ((current - user.getLastWake()) > REMOVE_AGE) {
                if (user.getUUID() != null) this.userCache.remove(user.getUUID().toUpperCase());
                if (user.getUsername() != null) this.userCache.remove(user.getUsername().toUpperCase());

                user.close();
            }
        }
    });

    private UserPlatform() {
        this.userDir.mkdirs();
        this.checkThread.start();
    }

    @SneakyThrows
    public User getUser(String identifier) throws IdentifierException {
        try {
            User user = this.userCache.get(identifier.toUpperCase());

            if (user == null) {
                switch (this) {
                    case CAFFEINE:
                        user = CaffeineUser.Unsafe.get(identifier);

                    case TWITCH:
                    case MIXER:
                    case YOUTUBE:
                    case CASTERLABS:
                    default: // Not ready
                        break;
                }

                if (user != null) {
                    if (user.getUUID() != null) this.userCache.put(user.getUUID().toUpperCase(), user);
                    if (user.getUsername() != null) this.userCache.put(user.getUsername().toUpperCase(), user);
                }
            } else {
                user.wake();
            }

            return user;
        } catch (CompletionException e) {
            throw new IdentifierException();
        }
    }

}
