package co.casterlabs.koi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import co.casterlabs.koi.config.KoiConfig;
import co.casterlabs.koi.external.StatsEndpoint;
import co.casterlabs.koi.integration.brime.BrimeIntegration;
import co.casterlabs.koi.integration.caffeine.CaffeineIntegration;
import co.casterlabs.koi.integration.glimesh.GlimeshIntegration;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.user.trovo.TrovoIntegration;
import co.casterlabs.koi.util.FileUtil;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.loggerimpl.FileLogHandler;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

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

        KoiImpl koi = new KoiImpl(config);

        Koi.setInstance(koi);

        if (config.isCaffeineEnabled()) {
            CaffeineIntegration integration = new CaffeineIntegration(config);

            koi.addPlatformIntegration(integration);
        }

        if (config.isTwitchEnabled()) {
            TwitchIntegration integration = new TwitchIntegration(config);

            koi.getServers().add(integration.getWebhookServer());

            koi.addPlatformIntegration(integration);
        }

        if (config.isTrovoEnabled()) {
            TrovoIntegration integration = new TrovoIntegration(config);

            koi.addPlatformIntegration(integration);
        }

        if (config.isGlimeshEnabled()) {
            GlimeshIntegration integration = new GlimeshIntegration(config);

            koi.addPlatformIntegration(integration);
        }

        if (config.isBrimeEnabled()) {
            BrimeIntegration integration = new BrimeIntegration(config);

            koi.addPlatformIntegration(integration);
        }

        koi.getServers().add(new StatsEndpoint(config.getStatsPort()));

        koi.start();
    }

}
