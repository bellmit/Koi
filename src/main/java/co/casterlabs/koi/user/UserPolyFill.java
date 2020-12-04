package co.casterlabs.koi.user;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.watercache.WaterCache;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public class UserPolyFill extends Cachable {
    private static final File DIR = new File(Koi.getInstance().getDir(), "preferences");
    private static final long EXPIRE = TimeUnit.MINUTES.toMillis(10);
    private static final long UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(6);
    private static final String[] COLORS = new String[] {
            "#FF0000",
            "#FF8000",
            "#FFFF00",
            "#80FF00",
            "#00FF00",
            "#00FF80",
            "#00FFFF",
            "#0080FF",
            "#0000FF",
            "#7F00FF",
            "#FF00FF",
            "#FF007F"
    };

    private static Map<String, UserPolyFill> preferences = new HashMap<>();
    private static WaterCache cache = new WaterCache();

    static {
        for (UserPlatform platform : UserPlatform.values()) {
            new File(DIR, platform.name()).mkdirs();
        }

        cache.start((long) (EXPIRE * .1));
    }

    private @Getter List<String> forcedBadges = new ArrayList<>();
    private final UserPlatform platform;
    private final String UUID;
    private final File file;
    private long lastUpdate;

    private Map<PolyFillRequirements, String> values = new ConcurrentHashMap<>();

    private UserPolyFill(UserPlatform platform, String UUID) {
        super(EXPIRE);

        this.platform = platform;
        this.UUID = UUID;
        this.file = new File(DIR, this.platform + "/" + this.UUID);

        cache.register(this);
        preferences.put(this.UUID, this);

        this.load();

        List<PolyFillRequirements> polys = PolyFillRequirements.getPolyFillForPlatform(this.platform);

        if (polys.contains(PolyFillRequirements.COLOR) && !this.values.containsKey(PolyFillRequirements.COLOR)) {
            this.set(PolyFillRequirements.COLOR, COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)]);
        }

        if (polys.contains(PolyFillRequirements.PROFILE_PICTURE) && !this.values.containsKey(PolyFillRequirements.PROFILE_PICTURE)) {
            this.update();
        }
    }

    @Override
    public void tick() {
        long current = System.currentTimeMillis();

        if ((current - this.lastUpdate) >= UPDATE_INTERVAL) {
            this.update();
        }
    }

    private void update() {
        List<PolyFillRequirements> polys = PolyFillRequirements.getPolyFillForPlatform(this.platform);

        this.lastUpdate = System.currentTimeMillis();

        if (polys.contains(PolyFillRequirements.PROFILE_PICTURE)) {
            try {
                SerializedUser user = Koi.getInstance().getUserSerialized(this.UUID, this.platform);

                this.set(PolyFillRequirements.PROFILE_PICTURE, user.getImageLink());

                FastLogger.logStatic(LogLevel.DEBUG, "Updated profile picture for %s;%s", this.UUID, this.platform);
            } catch (IdentifierException ignored) {
                ignored.printStackTrace();
            }
        }

        this.save();
    }

    private void load() {
        if (this.file.exists()) {
            try {
                JsonObject json = FileUtil.readJson(this.file, JsonObject.class);

                if (json.has("badges")) {
                    this.forcedBadges = Koi.GSON.fromJson(json.get("badges"), new TypeToken<List<String>>() {
                    }.getType());
                }

                if (json.has("last_update")) {
                    this.lastUpdate = json.get("last_update").getAsLong();
                }

                for (Entry<String, JsonElement> entry : json.entrySet()) {
                    try {
                        this.values.put(PolyFillRequirements.valueOf(entry.getKey().toUpperCase()), entry.getValue().getAsString());
                    } catch (Exception e) {}
                }
            } catch (IOException e) {
                FastLogger.logStatic(LogLevel.SEVERE, "Unable to load config for %s;%s", this.UUID, this.platform.name());
                FastLogger.logException(e);
            }
        }
    }

    @Override
    public boolean onDispose(DisposeReason reason) {
        preferences.remove(this.UUID);

        this.save();

        FastLogger.logStatic(LogLevel.DEBUG, "%s;%s was removed from preference cache", this.UUID, this.platform.name());

        return true;
    }

    public void set(PolyFillRequirements poly, String value) {
        this.values.put(poly, value);
        this.wake();
        this.save();
    }

    public String get(PolyFillRequirements poly) {
        this.wake();
        return this.values.get(poly);
    }

    public void wake() {
        long timeSince = System.currentTimeMillis() - this.getTimeCreated();

        this.life = timeSince + EXPIRE;
    }

    public void save() {
        try {
            JsonObject json = new JsonObject();

            json.add("badges", Koi.GSON.toJsonTree(this.forcedBadges));
            json.addProperty("last_update", this.lastUpdate);

            for (Map.Entry<PolyFillRequirements, String> entry : this.values.entrySet()) {
                json.addProperty(entry.getKey().name().toLowerCase(), entry.getValue());
            }

            FileUtil.writeJson(this.file, json);

            FastLogger.logStatic(LogLevel.DEBUG, "Saved config for %s;%s", this.UUID, this.platform.name());
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Unable to save config for %s;%s", this.UUID, this.platform.name());
            FastLogger.logException(e);
        }
    }

    public static UserPolyFill get(UserPlatform platform, String UUID) {
        UserPolyFill instance = preferences.get(UUID);

        if (instance == null) {
            instance = new UserPolyFill(platform, UUID);
        }

        return instance;
    }

}
