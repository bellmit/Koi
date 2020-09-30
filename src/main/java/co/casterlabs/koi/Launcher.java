package co.casterlabs.koi;

import co.casterlabs.koi.external.ChatEndpoint;
import co.casterlabs.koi.external.TwitchWebhookEndpoint;
import co.casterlabs.koi.user.caffeine.CaffeineAuth;
import co.casterlabs.koi.user.twitch.TwitchAuth;
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
            "-c",
            "--caffeine"
    }, description = "The Caffeine Refresh Token")
    private String caffeine;

    @Option(names = {
            "-tu",
            "--twitchusername"
    }, description = "The Twitch login name")
    private String twitchUsername;

    @Option(names = {
            "-tp",
            "--twitchpassword"
    }, description = "The Twitch oauth password")
    private String twitchPassword;

    @Option(names = {
            "-tc",
            "--twitchclientid"
    }, description = "The Twitch client id")
    private String twitchId;

    @Option(names = {
            "-ts",
            "--twitchsecret"
    }, description = "The Twitch secret")
    private String twitchSecret;

    @Option(names = {
            "-twhp",
            "--twitch-webhook-port"
    }, description = "The port to listen on for the webhooks endpoint")
    private int twitchPort = 9091;

    @Option(names = {
            "-twha",
            "--twitch-webhook-address"
    }, description = "The address to listen on for the webhooks endpoint")
    private String twitchAddress;

    @Option(names = {
            "-d",
            "--debug"
    }, description = "Enables debug logging")
    private boolean debug = false;

    @Option(names = {
            "-H",
            "--host"
    }, description = "The address to bind to")
    private String host = "127.0.0.1";

    @Option(names = {
            "-p",
            "--port"
    }, description = "The port to listen on")
    private int port = 8080;

    @Option(names = {
            "-cp",
            "--chat-port"
    }, description = "The port to listen on for the chat endpoint")
    private int chatPort = 9090;

    public static void main(String[] args) {
        new CommandLine(new Launcher()).execute(args);
    }

    @SneakyThrows
    @Override
    public void run() {
        if (this.debug) {
            FastLoggingFramework.setDefaultLevel(LogLevel.DEBUG);
            new FastLogger().debug("Debug mode enabled.");
        }

        // Set output to both console and latest.log
        new FileLogHandler();

        Koi koi = new Koi(this.host, this.port, this.debug, new CaffeineAuth(this.caffeine), (TwitchAuth) new TwitchAuth(this.twitchUsername, this.twitchPassword).login(this.twitchSecret, this.twitchId));

        koi.getServers().add(new ChatEndpoint(this.chatPort));
        koi.getServers().add(new TwitchWebhookEndpoint(this.twitchAddress, this.twitchPort));

        koi.start();
    }

}
