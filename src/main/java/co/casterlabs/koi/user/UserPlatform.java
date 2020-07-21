package co.casterlabs.koi.user;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

    private static Map<UserPlatform, UserProvider> providers = new HashMap<>();

    private @Getter Map<String, User> userCache = new ConcurrentHashMap<>();
    private @Getter File userDir = new File(Koi.getInstance().getDir(), "/users/" + this + "/");

    static {
        providers.put(UserPlatform.CAFFEINE, new CaffeineUser.Provider());

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

            // All users are in the cache twice, under their username and uuid.
            json.addProperty("listeners", listeners / 2);
            json.addProperty("cached", cached / 2);

            FileUtil.writeJson(STATS, json);
        }).start();
    }

    private UserPlatform() {
        this.userDir.mkdirs();
    }

    public User getUser(String identifier) throws IdentifierException {
        return this.getUser(identifier, null);
    }

    @SneakyThrows
    public User getUser(String identifier, Object data) throws IdentifierException {
        try {
            User user = this.userCache.get(identifier.toUpperCase());

            if (user == null) {
                user = providers.get(this).get(identifier, data);

                if (user != null) {
                    if (user.getUUID() != null) this.userCache.put(user.getUUID().toUpperCase(), user);
                    if (user.getUsername() != null) this.userCache.put(user.getUsername().toUpperCase(), user);
                }
            } else {
                user.updateUser(data);
                user.wake();
            }

            return user;
        } catch (Exception e) {
            throw new IdentifierException();
        }
    }

    public static UserPlatform parse(JsonElement platformJson) throws PlatformException {
        if (platformJson == null) {
            return UserPlatform.CAFFEINE;
        } else if (platformJson.isJsonPrimitive()) {
            for (UserPlatform platform : UserPlatform.values()) {
                if (platform.name().equalsIgnoreCase(platformJson.getAsString())) {
                    return platform;
                }
            }
        }

        throw new PlatformException();
    }

}
