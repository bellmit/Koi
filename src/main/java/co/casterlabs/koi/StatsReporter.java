package co.casterlabs.koi;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.koi.networking.SocketServer;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.FileUtil;

public class StatsReporter {
    //@formatter:off
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    //@formatter:on
    private static final Map<UserPlatform, StatsReporter> platforms = new HashMap<>();

    private Map<String, Set<String>> clientTypes = new ConcurrentHashMap<>();

    static {
        for (UserPlatform platform : UserPlatform.values()) {
            platforms.put(platform, new StatsReporter());
        }
    }

    public static StatsReporter get(UserPlatform platform) {
        return platforms.get(platform);
    }

    public static void saveStats() {
        JsonObject usernames = new JsonObject();
        JsonObject stats = new JsonObject();

        int count = 0;

        for (Entry<UserPlatform, StatsReporter> entry : platforms.entrySet()) {
            usernames.add(entry.getKey().name(), entry.getValue().serializeUsernames());
            count += entry.getValue().countUsernames();
        }

        stats.addProperty("users", count);
        stats.addProperty("listeners", SocketServer.getInstance().getConnections().size());

        FileUtil.write(new File("usernames.json"), PRETTY_GSON.toJson(usernames));
        FileUtil.write(new File("stats.json"), PRETTY_GSON.toJson(stats));
    }

    private int countUsernames() {
        Set<String> count = new HashSet<>();

        for (Entry<String, Set<String>> entry : new ArrayList<>(this.clientTypes.entrySet())) {
            count.addAll(entry.getValue());
        }

        return count.size();
    }

    private JsonObject serializeUsernames() {
        JsonObject json = new JsonObject();

        for (Entry<String, Set<String>> entry : new ArrayList<>(this.clientTypes.entrySet())) {
            json.add(entry.getKey(), Koi.GSON.toJsonTree(entry.getValue()));
        }

        return json;
    }

    public void registerConnection(String username, String clientType) {
        Set<String> users = this.clientTypes.get(clientType);

        if (users == null) {
            users = new HashSet<>();

            this.clientTypes.put(clientType, users);
        }

        users.add(username);
    }

    public void unregisterConnection(String username, String clientType) {
        Set<String> users = this.clientTypes.getOrDefault(clientType, Collections.emptySet());

        users.remove(username);

        if (users.isEmpty()) {
            this.clientTypes.remove(clientType);
        }
    }

}
