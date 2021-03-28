package co.casterlabs.koi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.ApiUtil;
import co.casterlabs.apiutil.ErrorReporter;
import co.casterlabs.brimeapijava.BrimeApi;
import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.external.ThirdPartyBroadcastEndpoint;
import co.casterlabs.koi.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.brime.BrimeAppAuth;
import co.casterlabs.koi.user.glimesh.GlimeshApplicationAuth;
import co.casterlabs.koi.user.trovo.TrovoApplicationAuth;
import co.casterlabs.koi.user.twitch.TwitchCredentialsAuth;
import co.casterlabs.koi.util.FileUtil;
import co.casterlabs.twitchapi.helix.CheermoteCache;
import co.casterlabs.twitchapi.helix.TwitchHelixAuth;
import lombok.NonNull;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.loggerimpl.FileLogHandler;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.fastloggingframework.logging.LoggingUtil;

@Command(name = "start", mixinStandardHelpOptions = true, version = "Koi v" + Koi.VERSION, description = "Starts the Koi server")
public class Launcher implements Runnable {

    @Option(names = {
            "-d",
            "--debug"
    }, description = "Enables debug logging")
    private boolean debug = false;

    static {
        try {
            File file = new File("errors.log");

            file.createNewFile();

            // Delete old log and then set error output to both console and latest.log
            new File("latest.log").delete();
            new FileLogHandler();

            @SuppressWarnings("resource")
            FileOutputStream fileOut = new FileOutputStream(file);
            OutputStream errOut = System.err;

            System.setErr(new PrintStream(new OutputStream() {
                @Override
                public void write(int value) throws IOException {
                    errOut.write(value);
                    fileOut.write(value);
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Launcher()).execute(args);
    }

    @SuppressWarnings("resource")
    @SneakyThrows
    @Override
    public void run() {
        File file = new File("config.json");
        KoiConfig config;

        if (file.exists()) {
            config = FileUtil.readJson(file, KoiConfig.class);
        } else {
            config = new KoiConfig();

            FileUtil.writeJson(file, Koi.GSON.toJsonTree(config));
        }

        if (this.debug) {
            FastLoggingFramework.setDefaultLevel(LogLevel.TRACE);
            new FastLogger().debug("Debug mode enabled.");
            config.setDebugModeEnabled(true);
        }

        ApiUtil.setErrorReporter(new ErrorReporter() {
            @Override
            public void apiError(@NonNull String url, @Nullable String sentBody, @Nullable Object sentHeaders, @Nullable String recBody, @Nullable Object recHeaders, @NonNull Throwable t) {
                ErrorReporting.apierror(LoggingUtil.getCallingClass(), url, sentBody, sentHeaders, recBody, recHeaders, t);
            }
        });

        Koi koi = new Koi(config);

        if (config.isCaffeineEnabled()) {
            Koi.getInstance().getLogger().info("Enabled Caffeine support.");
        }

        if (config.isTwitchEnabled()) {
            koi.addAuthProvider(new TwitchCredentialsAuth(config.getTwitchSecret(), config.getTwitchId()));
            koi.getServers().add(new TwitchWebhookEndpoint(config.getTwitchAddress(), config.getTwitchPort()));

            new RepeatingThread("Twitch Cheermote Refresh - Koi", TimeUnit.HOURS.toMillis(1), () -> {
                try {
                    CheermoteCache.update((TwitchHelixAuth) koi.getAuthProvider(UserPlatform.TWITCH)).join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            Koi.getInstance().getLogger().info("Enabled Twitch support.");
        }

        if (config.isTrovoEnabled()) {
            koi.addAuthProvider(new TrovoApplicationAuth(config.getTrovoId()));

            Koi.getInstance().getLogger().info("Enabled Trovo support.");
        }

        if (config.isBrimeEnabled()) {
            koi.addAuthProvider(new BrimeAppAuth(config.getBrimeClientId()));

            BrimeApi.targetApiEndpoint = BrimeApi.STAGING_API;

            Koi.getInstance().getLogger().info("Enabled Brime support.");
        }

        koi.getServers().add(new ThirdPartyBroadcastEndpoint(config.getThirdPartyPort()));

        if (config.isGlimeshEnabled()) {
            koi.addAuthProvider(new GlimeshApplicationAuth(config.getGlimeshId()));

            Koi.getInstance().getLogger().info("Enabled Glimesh support.");
        }

        koi.start();
    }

}
