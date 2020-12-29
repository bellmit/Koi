package co.casterlabs.koi;

import java.io.File;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.ApiUtil;
import co.casterlabs.apiutil.ErrorReporter;
import co.casterlabs.koi.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.twitch.TwitchCredentialsAuth;
import co.casterlabs.koi.util.FileUtil;
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

    public static void main(String[] args) {
        new CommandLine(new Launcher()).execute(args);
    }

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
            FastLoggingFramework.setDefaultLevel(LogLevel.DEBUG);
            new FastLogger().debug("Debug mode enabled.");
            config.setDebugModeEnabled(true);
        }

        ApiUtil.setErrorReporter(new ErrorReporter() {
            @Override
            public void apiError(@NonNull String url, @Nullable String sentBody, @Nullable Object sentHeaders, @Nullable String recBody, @Nullable Object recHeaders, @NonNull Throwable t) {
                ErrorReporting.apierror(LoggingUtil.getCallingClass(), url, sentBody, sentHeaders, recBody, recHeaders, t);
            }
        });

        // Set output to both console and latest.log
        new FileLogHandler();

        Koi koi = new Koi(config);

        if (config.isCaffeineEnabled()) {
            Koi.getInstance().getLogger().info("Enabled Caffeine support.");
        }

        if (config.isTwitchEnabled()) {
            koi.addAuthProvider(new TwitchCredentialsAuth(config.getTwitchUsername(), config.getTwitchPassword(), config.getTwitchSecret(), config.getTwitchId()));
            koi.getServers().add(new TwitchWebhookEndpoint(config.getTwitchAddress(), config.getTwitchPort()));

            Koi.getInstance().getLogger().info("Enabled Twitch support.");
        }

        koi.start();
    }

}
