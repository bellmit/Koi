package co.casterlabs.koi.user;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.koi.IdentifierException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.user.caffeine.CaffeineUser;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import lombok.SneakyThrows;

public enum UserPlatform {
    CAFFEINE;

    private static final long REMOVE_AGE = TimeUnit.MINUTES.toMillis(5);
    private static final File STATS = new File("stats.json");
    private static final long REPEAT = 5000; // 5s

    private @Getter Map<String, User> userCache = new ConcurrentHashMap<>();
    private @Getter File userDir = new File(Koi.getInstance().getDir(), "/users/" + this + "/");

    static {
        new RepeatingThread(REPEAT, () -> {
            long listeners = 0;
            long cached = 0;

            for (UserPlatform platform : UserPlatform.values()) {
                long current = System.currentTimeMillis();

                cached += platform.userCache.size();

                for (User user : platform.userCache.values()) {
                    if (user.hasListeners()) {
                        listeners += user.getEventListeners().size();
                        user.update();
                    } else {
                        long age = current - user.getLastWake();

                        if (Koi.getInstance().isDebug() || (age > REMOVE_AGE)) {
                            if (user.getUUID() != null) platform.userCache.remove(user.getUUID().toUpperCase());
                            if (user.getUsername() != null) platform.userCache.remove(user.getUsername().toUpperCase());

                            user.close();
                        }
                    }
                }
            }

            JsonObject json = new JsonObject();

            json.addProperty("listeners", listeners);
            json.addProperty("cached", cached);

            FileUtil.writeJson(STATS, json);
        }).start();
    }

    private UserPlatform() {
        this.userDir.mkdirs();
    }

    @SneakyThrows
    public User getUser(String identifier) throws IdentifierException {
        try {
            User user = this.userCache.get(identifier.toUpperCase());

            if (user == null) {
                switch (this) {
                    case CAFFEINE:
                        user = CaffeineUser.Unsafe.get(identifier);
                        break;

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
        } catch (Exception e) {
            e.printStackTrace();
            throw new IdentifierException();
        }
    }

}
