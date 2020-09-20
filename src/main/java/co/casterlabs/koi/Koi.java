package co.casterlabs.koi;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.koi.external.ChatEndpoint;
import co.casterlabs.koi.networking.SocketServer;
import co.casterlabs.koi.serialization.SerializedUserSerializer;
import co.casterlabs.koi.serialization.UserSerializer;
import co.casterlabs.koi.status.StatusReporter;
import co.casterlabs.koi.user.AuthProvider;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.caffeine.CaffeineStatus;
import co.casterlabs.koi.user.caffeine.CaffeineUserConverter;
import co.casterlabs.koi.user.twitch.TwitchUserConverter;
import co.casterlabs.koi.util.CurrencyUtil;
import co.casterlabs.koi.util.FileUtil;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Koi {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(SerializedUser.class, new SerializedUserSerializer()).registerTypeAdapter(User.class, new UserSerializer()).create();
    public static final String VERSION = "1.12.0";
    private static final File STATUS = new File("status.json");

    private static @Getter ThreadPoolExecutor outgoingThreadPool = new ThreadPoolExecutor(8, 16, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter ThreadPoolExecutor eventThreadPool = new ThreadPoolExecutor(16, 32, 480, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter ThreadPoolExecutor miscThreadPool = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter Koi instance;

    // Koi things
    private Map<UserPlatform, AuthProvider> authProviders = new ConcurrentHashMap<>();
    private Set<StatusReporter> statusReporters = new HashSet<>();
    private @Getter FastLogger logger = new FastLogger();
    private @Getter File dir = new File("koi");
    private @Getter boolean debug;
    private SocketServer server;
    private ChatEndpoint chat;

    private RepeatingThread statusThread = new RepeatingThread(UserPlatform.REPEAT, () -> {
        JsonObject json = new JsonObject();

        for (StatusReporter status : this.statusReporters) {
            JsonObject section = new JsonObject();

            status.report(section);

            json.add(status.getName(), section);
        }

        FileUtil.writeJson(STATUS, json);
    });

    public Koi(String host, int port, boolean debug, ChatEndpoint chat, AuthProvider... authProviders) {
        if ((instance == null) || !instance.isRunning()) {
            instance = this;
        } else {
            throw new IllegalStateException("Koi is running, stop it in order to instantiate.");
        }

        for (AuthProvider provider : authProviders) {
            this.authProviders.put(provider.getPlatform(), provider);
        }

        this.debug = debug;
        this.server = new SocketServer(new InetSocketAddress(host, port), this);
        this.chat = chat;

        CurrencyUtil.init();

        this.dir.mkdirs();

        this.statusReporters.add(CaffeineStatus.getInstance());
    }

    private boolean isRunning() {
        return this.server.isRunning();
    }

    public AuthProvider getAuthProvider(UserPlatform platform) {
        return this.authProviders.get(platform);
    }

    @SneakyThrows
    public void start() {
        this.server.start();
        this.chat.start();

        InetSocketAddress address = this.server.getAddress();
        this.logger.info(String.format("Koi started on %s:%d!", address.getHostString(), address.getPort()));
        this.logger.info(String.format("Chat endpoint started on port %d!", this.chat.getListeningPort()));

        if (WebUtil.isUsingProxy()) {
            String publicIp = WebUtil.sendHttpGet("https://api.ipify.org/", null);
            this.logger.info("Public address is %s.", publicIp);
        }

        this.statusThread.start();
    }

    public void stop() {
        this.server.stop();
        this.chat.stop();
        this.statusThread.stop();
    }

    @SneakyThrows
    public User getUser(String identifier, UserPlatform platform) throws IdentifierException {
        return platform.getUser(identifier);
    }

    public User getUser(String identifier, UserPlatform platform, Object data) throws IdentifierException {
        return platform.getUser(identifier, data);
    }

    public SerializedUser getUserSerialized(String UUID, UserPlatform platform) throws IdentifierException {
        switch (platform) {
            case CAFFEINE:
                return CaffeineUserConverter.getInstance().get(UUID);

            case TWITCH:
                return TwitchUserConverter.getInstance().get(UUID);

            default:
                return null;
        }
    }

}
