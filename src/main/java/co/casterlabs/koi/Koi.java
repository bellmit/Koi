package co.casterlabs.koi;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import co.casterlabs.koi.networking.Server;
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
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.consolidate.CommandRegistry;
import xyz.e3ndr.consolidate.exception.ArgumentsLengthException;
import xyz.e3ndr.consolidate.exception.CommandExecutionException;
import xyz.e3ndr.consolidate.exception.CommandNameException;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Koi {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(SerializedUser.class, new SerializedUserSerializer()).registerTypeAdapter(User.class, new UserSerializer()).create();
    public static final String VERSION = "1.13.3";
    private static final File STATUS = new File("status.json");

    private static @Getter ThreadPoolExecutor outgoingThreadPool = new ThreadPoolExecutor(8, 16, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter ThreadPoolExecutor eventThreadPool = new ThreadPoolExecutor(16, 32, 480, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter ThreadPoolExecutor miscThreadPool = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private static @Getter Koi instance;

    // Koi things
    private Map<UserPlatform, AuthProvider> authProviders = new ConcurrentHashMap<>();
    private CommandRegistry<Void> commandRegistry = new CommandRegistry<>();
    private Set<StatusReporter> statusReporters = new HashSet<>();
    private @Getter Set<Server> servers = new HashSet<>();
    private @Getter FastLogger logger = new FastLogger();
    private @Getter File dir = new File("koi");
    private @Getter boolean debug;

    private RepeatingThread statusThread = new RepeatingThread(UserPlatform.REPEAT, () -> {
        JsonObject json = new JsonObject();

        for (StatusReporter status : this.statusReporters) {
            JsonObject section = new JsonObject();

            status.report(section);

            json.add(status.getName(), section);
        }

        FileUtil.writeJson(STATUS, json);
    });

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
        this.servers.add(new SocketServer(new InetSocketAddress(host, port), this));

        CurrencyUtil.init();

        this.dir.mkdirs();

        this.statusReporters.add(CaffeineStatus.getInstance());

        this.commandRegistry.addCommand(new KoiCommands(this.commandRegistry));
        (new Thread() {
            @SuppressWarnings("resource")
            @Override
            public void run() {
                Scanner in = new Scanner(System.in);

                while (true) {
                    try {
                        commandRegistry.execute(in.nextLine());
                    } catch (CommandNameException | CommandExecutionException | ArgumentsLengthException e) {
                        e.printStackTrace();
                        logger.exception(e);
                    }
                }
            }
        }).start();
    }

    public boolean isRunning() {
        for (Server server : this.servers) {
            if (!server.isRunning()) {
                return false;
            }
        }

        return true;
    }

    public AuthProvider getAuthProvider(UserPlatform platform) {
        return this.authProviders.get(platform);
    }

    @SneakyThrows
    public void start() {
        if (!this.isRunning()) {
            this.statusThread.start();
            this.servers.forEach(Server::start);
        }
    }

    public void stop() {
        if (this.isRunning()) {
            this.statusThread.stop();

            this.servers.forEach(Server::stop);

            this.logger.info("Stopped koi!");
        }
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
