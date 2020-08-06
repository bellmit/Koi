package co.casterlabs.koi.user;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.util.FileUtil;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.watercache.WaterCache;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public class UserPreferences extends Cachable {
    private static final long EXPIRE = TimeUnit.MINUTES.toMillis(15);
    private static final File DIR = new File(Koi.getInstance().getDir(), "preferences");
    private static final String[] COLORS = new String[] { "#FF0000", "#FF8000", "#FFFF00", "#80FF00", "#00FF00", "#00FF80", "#00FFFF", "#0080FF", "#0000FF", "#7F00FF", "#FF00FF", "#FF007F"
    };
    
    private static WaterCache cache = new WaterCache();
    private static Map<String, UserPreferences> preferences = new HashMap<>();
    
    static {
        for (UserPlatform platform : UserPlatform.values()) {
            new File(DIR, platform.name()).mkdirs();
        }

        cache.start((long) (EXPIRE * .1));
    }
    
    private final UserPlatform platform;
    private final String UUID;
    private final File file;
    private String color;
    
    private UserPreferences(UserPlatform platform, String UUID) {
        super(EXPIRE);
        
        this.platform = platform;
        this.UUID = UUID;
        this.file = new File(DIR, this.platform + "/" + this.UUID);

        cache.register(this);
        preferences.put(this.UUID, this);

        if (this.file.exists()) {
            try {
                JsonObject json = FileUtil.readJson(this.file, JsonObject.class);
                
                this.color = json.get("color").getAsString();
            } catch (IOException e) {
                FastLogger.logStatic(LogLevel.SEVERE, "Unable to load config for %s;%s", this.UUID, this.platform.name());
                FastLogger.logException(e);
            }
        } else {
            this.color = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
            this.save();
        }
    }

    @Override
    public boolean onDispose(DisposeReason reason) {
        preferences.remove(this.UUID);
        
        this.save();
        
        FastLogger.logStatic(LogLevel.DEBUG, "%s;%s was removed from preference cache", this.UUID, this.platform.name());
        
        return true;
    }

    public void setColor(String color) {
        this.color = color;

        this.wake();
        this.save();
    }
    
    public String getColor() {
        this.wake();
        return this.color;
    }
    
    public void wake() {
        long timeSince = System.currentTimeMillis() - this.getTimeCreated();
        
        this.life = timeSince + EXPIRE;
    }
    
    public void save() {
        try {
            JsonObject json = new JsonObject();
            
            json.addProperty("color", this.color);
            
            FileUtil.writeJson(this.file, json);
            
            FastLogger.logStatic(LogLevel.DEBUG, "Saved config for %s;%s", this.UUID, this.platform.name());
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Unable to save config for %s;%s", this.UUID, this.platform.name());
            FastLogger.logException(e);
        }
    }

    public static UserPreferences get(UserPlatform platform, String UUID) {
        UserPreferences instance = preferences.get(UUID);
        
        if (instance == null) {
            instance = new UserPreferences(platform, UUID);
        }
        
        return instance;
    }
    
}
