package co.casterlabs.koi.user;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.EventListener;
import co.casterlabs.koi.networking.SocketClient;
import co.casterlabs.koi.user.caffeine.CaffeineUser;
import co.casterlabs.koi.user.twitch.TwitchUser;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@RequiredArgsConstructor
public enum UserPlatform {
    CAFFEINE("https://caffeine.tv/%s"),
    TWITCH("https://twitch.tv/%s");

    private static final long REMOVE_AGE = TimeUnit.MINUTES.toMillis(5);
    private static final File USERNAMES = new File("usernames.json");
    private static final File STATS = new File("stats.json");
    public static final long REPEAT = TimeUnit.SECONDS.toMillis(5);

    private static Map<UserPlatform, UserProvider> providers = new HashMap<>();

    private @Getter Map<String, User> userCache = new ConcurrentHashMap<>();
    private @Getter File userDir = new File("koi/users/", this.name());
    private @NonNull String platformLink;

    static {
        new RepeatingThread("Event Loop - Koi", REPEAT, () -> {
            JsonObject platforms = new JsonObject(); // For internal tracking.
            long listeners = 0;
            long usercount = 0;

            for (UserPlatform platform : UserPlatform.values()) {
                JsonObject usernames = new JsonObject();
                long current = System.currentTimeMillis();

                for (User user : new ArrayList<>(platform.userCache.values())) {
                    try {
                        if (user.hasListeners()) {
                            JsonArray clientTypes = new JsonArray();

                            listeners += user.getEventListeners().size();
                            usercount++;

                            for (EventListener listener : user.getEventListeners()) {
                                if (listener instanceof SocketClient) {
                                    String type = ((SocketClient) listener).getClientType();

                                    if (type.contains("Caffeinated")) {
                                        clientTypes.add("Caffeinated");
                                    } else {
                                        clientTypes.add(type);
                                    }
                                } else {
                                    clientTypes.add("Unknown");
                                }
                            }

                            usernames.add(user.getUsername(), clientTypes);
                            user.update();
                        } else {
                            long age = current - user.getLastWake();

                            if (Koi.getInstance().isDebug() || (age > REMOVE_AGE)) {
                                if (user.getUUID() != null) platform.userCache.remove(user.getUUID().toUpperCase());
                                if (user.getUsername() != null) platform.userCache.remove(user.getUsername().toUpperCase());

                                user.close();
                            }
                        }
                    } catch (Exception e) {
                        FastLogger.logStatic(LogLevel.SEVERE, "Ticking %s;%s produced an exception:", user.getUUID(), user.getPlatform());
                        FastLogger.logException(e);
                    }
                }

                platforms.add(platform.name(), usernames);
            }

            JsonObject statsJson = new JsonObject();

            statsJson.addProperty("listeners", listeners / 2);
            statsJson.addProperty("users", usercount / 2);

            FileUtil.writeJson(STATS, statsJson);
            FileUtil.writeJson(USERNAMES, platforms);
        }).start();
    }

    public static int removeAll() {
        int count = 0;

        for (UserPlatform platform : UserPlatform.values()) {
            for (User user : new HashSet<>(platform.userCache.values())) {
                if (user.getUUID() != null) platform.userCache.remove(user.getUUID().toUpperCase());
                if (user.getUsername() != null) platform.userCache.remove(user.getUsername().toUpperCase());
                count++;
                user.close();
            }
        }

        return count;
    }

    public static void init() {
        providers.clear();

        if (Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE) != null) {
            providers.put(UserPlatform.CAFFEINE, new CaffeineUser.Provider());
        }

        if (Koi.getInstance().getAuthProvider(UserPlatform.TWITCH) != null) {
            providers.put(UserPlatform.TWITCH, new TwitchUser.Provider());
        }
    }

    public static Set<User> getAll() {
        Set<User> result = new HashSet<>();

        for (UserPlatform platform : UserPlatform.values()) {
            result.addAll(platform.userCache.values());
        }

        return result;
    }

    private UserPlatform() {
        this.userDir.mkdirs();
    }

    public User getUser(String identifier) throws IdentifierException {
        return this.getUser(identifier, null);
    }

    public String getLinkForUser(String username) {
        return String.format(this.platformLink, username);
    }

    @SneakyThrows
    public User getUser(String identifier, Object data) throws IdentifierException {
        try {
            User user = this.userCache.get(identifier.toUpperCase());

            if (user == null) {
                user = providers.get(this).get(identifier, data);

                if (user != null) {
                    if (user.getUsername() != null) this.userCache.put(user.getUsername().toUpperCase(), user);
                    if (user.getUUID() != null) this.userCache.put(user.getUUID().toUpperCase(), user);
                }
            } else {
                user.updateUser(data);
                user.wake();
            }

            return user;
        } catch (Exception e) {
            e.printStackTrace();
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

    public static UserPlatform parse(JsonElement platformJson, String username) throws PlatformException {
        String[] split = username.split(";");

        if (split.length == 2) {
            for (UserPlatform platform : UserPlatform.values()) {
                if (platform.name().equalsIgnoreCase(split[1])) {
                    return platform;
                }
            }
        } else {
            return parse(platformJson);
        }

        return UserPlatform.CAFFEINE;
    }

    public static UserPlatform parse(String str) throws PlatformException {
        for (UserPlatform platform : UserPlatform.values()) {
            if (platform.name().equalsIgnoreCase(str)) {
                return platform;
            }
        }

        throw new PlatformException();
    }

}
