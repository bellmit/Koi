package co.casterlabs.koi;

import java.net.InetSocketAddress;
import java.net.Proxy;

import co.casterlabs.koi.external.ChatEndpoint;
import co.casterlabs.koi.user.caffeine.CaffeineAuth;
import co.casterlabs.koi.user.twitch.TwitchAuth;
import co.casterlabs.koi.util.WebUtil;
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
            "-d",
            "--debug"
    }, description = "Enables debug logging")
    private boolean debug = false;

    @Option(names = {
            "-S",
            "--socks"
    }, description = "The socks proxy address to use, no proxy is used by default")
    private String socksProxy;

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

        if (this.socksProxy != null) {
            String[] proxy = this.socksProxy.split(":");
            int proxyPort = (proxy.length > 0) ? Integer.parseInt(proxy[1]) : 9050;

            WebUtil.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy[0], proxyPort)));
        }

        // Set output to both console and latest.log
        new FileLogHandler();

        Koi koi = new Koi(this.host, this.port, this.debug, new ChatEndpoint(this.chatPort), new CaffeineAuth(this.caffeine), new TwitchAuth(this.twitchUsername, this.twitchPassword, this.twitchId));

        koi.start();
    }

}
