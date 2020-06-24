package co.casterlabs.koi;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.koi.networking.SocketServer;
import co.casterlabs.koi.serialization.UserSerializer;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.CurrencyUtil;
import co.casterlabs.koi.util.WebUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.FastLoggingFramework.Logging.FastLogger;

public class Koi {
    public static final String VERSION = "1.4.0";
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(User.class, new UserSerializer()).create();

    private static @Getter ThreadPoolExecutor outgoingThreadPool = new ThreadPoolExecutor(8, 16, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter ThreadPoolExecutor eventThreadPool = new ThreadPoolExecutor(16, 32, 480, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter ThreadPoolExecutor miscThreadPool = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter Koi instance;

    // Koi things
    private Map<UserPlatform, AuthProvider> authProviders = new ConcurrentHashMap<>();
    private @Getter FastLogger logger = new FastLogger();
    private @Getter File dir = new File("koi");
    private @Getter boolean debug;
    private SocketServer server;

    public Koi(String host, int port, boolean debug, AuthProvider... authProviders) {
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

        CurrencyUtil.init();

        this.dir.mkdirs();
    }

    private boolean isRunning() {
        return this.server.isRunning();
    }

    public AuthProvider getAuthProvider(UserPlatform platform) {
        return this.authProviders.get(platform);
    }

    public void start() {
        this.server.start();

        InetSocketAddress address = this.server.getAddress();
        this.logger.info(String.format("Koi started on %s:%d!", address.getHostString(), address.getPort()));

        if (WebUtil.isUsingProxy()) {
            String publicIp = WebUtil.sendHttpGet("https://api.ipify.org/", null);
            this.logger.info(String.format("Public address is %s.", publicIp));
        }
    }

    public void stop() {
        this.server.stop();
    }

    @SneakyThrows
    public User getUser(String identifier, UserPlatform platform) throws IdentifierException {
        return platform.getUser(identifier);
    }

    @SneakyThrows
    public User getUser(String identifier, UserPlatform platform, JsonObject json) throws IdentifierException {
        return platform.getUser(identifier, json);
    }

}
