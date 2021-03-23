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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.clientid.ClientIdMeta;
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

    private Map<String, Set<String>> clientIds = new ConcurrentHashMap<>();

    static {
        for (UserPlatform platform : UserPlatform.values()) {
            if (platform != UserPlatform.CASTERLABS_SYSTEM) {
                platforms.put(platform, new StatsReporter());
            }
        }
    }

    private int countUsernames() {
        Set<String> count = new HashSet<>();

        for (Entry<String, Set<String>> entry : new ArrayList<>(this.clientIds.entrySet())) {
            count.addAll(entry.getValue());
        }

        return count.size();
    }

    private JsonObject serializeUsernames(JsonObject publicStats) {
        JsonObject json = new JsonObject();

        for (Entry<String, Set<String>> entry : new ArrayList<>(this.clientIds.entrySet())) {
            String clientId = entry.getKey();
            Set<String> usernames = entry.getValue();
            ClientIdMeta meta = Natsukashii.getClientIdMeta(clientId);

            if (meta == null) {
                meta = ClientIdMeta.UNKNOWN;
            }

            if (!meta.isNonLogging()) {
                json.add(meta.getName(), Koi.GSON.toJsonTree(usernames));
            }

            String property = meta.isShowingPublicStats() ? meta.getName() : "OTHER";
            JsonElement countElement = publicStats.get(property);
            int count = usernames.size();

            if (countElement != null) {
                count += countElement.getAsInt();
            }

            publicStats.addProperty(property, count);
        }

        return json;
    }

    public void registerConnection(String username, String clientId) {
        Set<String> users = this.clientIds.get(clientId);

        if (users == null) {
            users = new HashSet<>();

            this.clientIds.put(clientId, users);
        }

        users.add(username);
    }

    public void unregisterConnection(String username, String clientId) {
        Set<String> users = this.clientIds.getOrDefault(clientId, Collections.emptySet());

        users.remove(username);

        if (users.isEmpty()) {
            this.clientIds.remove(clientId);
        }
    }

    /* ---------------- */
    /* Static           */
    /* ---------------- */

    public static StatsReporter get(UserPlatform platform) {
        return platforms.get(platform);
    }

    public static void saveStats() {
        JsonObject usernames = new JsonObject();
        JsonObject stats = new JsonObject();
        JsonObject publicStats = new JsonObject();

        int count = 0;

        for (Entry<UserPlatform, StatsReporter> entry : platforms.entrySet()) {
            usernames.add(entry.getKey().name(), entry.getValue().serializeUsernames(publicStats));
            count += entry.getValue().countUsernames();
        }

        stats.addProperty("connections", SocketServer.getInstance().getConnections().size());
        stats.addProperty("users", count);
        stats.add("stats", publicStats);

        FileUtil.write(new File("usernames.json"), PRETTY_GSON.toJson(usernames));
        FileUtil.write(new File("stats.json"), PRETTY_GSON.toJson(stats));
    }

}
