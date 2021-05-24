package co.casterlabs.koi;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import co.casterlabs.koi.config.BadgeConfiguration;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.serialization.InstantSerializer;
import co.casterlabs.koi.serialization.UserSerializer;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import lombok.Getter;
import lombok.Setter;

public abstract class Koi {
    public static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .registerTypeAdapter(User.class, new UserSerializer())
        .registerTypeAdapter(Instant.class, new InstantSerializer())
        .create();

    public static final String VERSION = "2.35.1";

    public static final ThreadPoolExecutor eventThreadPool = new ThreadPoolExecutor(16, 128, 480, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    public static final ThreadPoolExecutor clientThreadPool = new ThreadPoolExecutor(4, 16, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    public static final ThreadPoolExecutor miscThreadPool = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private static @Setter @Getter Koi instance = null;

    /* ---------------- */
    /* Interface        */
    /* ---------------- */

    public abstract KoiConfig getConfig();

    public abstract BadgeConfiguration getForcedBadges(UserPlatform platform, String uuid);

    public abstract UserConverter<?> getUserConverter(UserPlatform platform);

    public abstract UserProvider getUserProvider(UserPlatform platform);

    public abstract PlatformAuthorizer getPlatformAuthorizer(UserPlatform platform);

}
