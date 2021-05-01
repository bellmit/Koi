package co.casterlabs.koi;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.config.BadgeConfiguration;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.external.Server;
import co.casterlabs.koi.networking.SocketServer;
import co.casterlabs.koi.networking.outgoing.ClientBannerNotice;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;
import co.casterlabs.koi.util.FileUtil;
import co.casterlabs.koi.util.RepeatingThread;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class KoiImpl extends Koi {
    private Map<UserPlatform, UserConverter<?>> userConverters = new HashMap<>();
    private Map<UserPlatform, UserProvider> userProviders = new HashMap<>();
    private Map<UserPlatform, PlatformAuthorizer> platformAuthorizers = new HashMap<>();

    private @Getter Map<String, BadgeConfiguration> forcedBadges = new HashMap<>();
    private @Getter Set<Server> servers = new HashSet<>();

    private @Getter FastLogger logger = new FastLogger();
    private @Getter KoiConfig config;

    private @Getter ClientBannerNotice[] notices = new ClientBannerNotice[0];

    private static @Getter KoiImpl instance;

    static {
        eventThreadPool.setThreadFactory(new ThreadFactory() {
            private long threadNum = 0;

            @Override
            public Thread newThread(Runnable run) {
                Thread t = new Thread(run);

                t.setName("Event Thread Pool - Koi #" + this.threadNum++);

                return t;
            }
        });

        miscThreadPool.setThreadFactory(new ThreadFactory() {
            private long threadNum = 0;

            @Override
            public Thread newThread(Runnable run) {
                Thread t = new Thread(run);

                t.setName("Misc Thread Pool - Koi #" + this.threadNum++);

                return t;
            }
        });

    }

    @SuppressWarnings("resource")
    public KoiImpl(KoiConfig config) {
        if (instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException("Koi has already been instantiated.");
        }

        this.config = config;

        this.servers.add(new SocketServer(new InetSocketAddress(this.config.getHost(), this.config.getPort()), this));

        new RepeatingThread("User Stats - Koi", TimeUnit.SECONDS.toMillis(15), () -> {
            StatsReporter.saveStats();
        }).start();

        this.reloadBadges();
        this.reloadNotices();
    }

    public void reloadNotices() {
        try {
            JsonArray array = FileUtil.readJson(new File("notices.json"), JsonArray.class);

            ClientBannerNotice[] newNotices = new ClientBannerNotice[array.size()];

            for (int i = 0; i < array.size(); i++) {
                newNotices[i] = new ClientBannerNotice(array.get(i));
            }

            this.notices = newNotices;

            SocketServer.getInstance().sendNotices();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadBadges() {
        try {
            JsonObject badges = FileUtil.readJson(new File("badges.json"), JsonObject.class);

            this.forcedBadges.clear();

            for (Map.Entry<String, JsonElement> entry : badges.entrySet()) {
                this.forcedBadges.put(entry.getKey(), Koi.GSON.fromJson(entry.getValue(), BadgeConfiguration.class));
            }
        } catch (IOException e) {}
    }

    public boolean isRunning() {
        for (Server server : this.servers) {
            if (!server.isRunning()) {
                return false;
            }
        }

        return true;
    }

    @SneakyThrows
    public void start() {
        if (!this.isRunning()) {
            this.servers.forEach(Server::start);
        }
    }

    public void stop() {
        if (this.isRunning()) {
            this.servers.forEach(Server::stop);

            this.logger.info("Stopped koi!");
        }
    }

    @Override
    public @NonNull BadgeConfiguration getForcedBadges(@NonNull UserPlatform platform, @NonNull String UUID) {
        return this.forcedBadges.get(UUID + ";" + platform);
    }

    @Override
    public UserConverter<?> getUserConverter(UserPlatform platform) {
        return this.userConverters.get(platform);
    }

    @Override
    public UserProvider getUserProvider(UserPlatform platform) {
        return this.userProviders.get(platform);
    }

    @Override
    public PlatformAuthorizer getPlatformAuthorizer(UserPlatform platform) {
        return this.platformAuthorizers.get(platform);
    }

    public void addPlatformIntegration(PlatformIntegration integration) {
        this.userConverters.put(integration.getPlatform(), integration.getUserConverter());
        this.userProviders.put(integration.getPlatform(), integration.getUserProvider());
        this.platformAuthorizers.put(integration.getPlatform(), integration.getPlatformAuthorizer());
    }

}
