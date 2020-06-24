package co.casterlabs.koi;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import co.casterlabs.koi.user.caffeine.CaffeineAuth;
import co.casterlabs.koi.util.WebUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import xyz.e3ndr.FastLoggingFramework.FastLoggingFramework;
import xyz.e3ndr.FastLoggingFramework.Logging.FastLogger;
import xyz.e3ndr.FastLoggingFramework.Logging.LogLevel;

@Command(name = "start", mixinStandardHelpOptions = true, version = "Koi v" + Koi.VERSION, description = "Starts the Koi server")
public class Launcher implements Runnable {

    @Option(names = { "-c", "--caffeine"
    }, description = "The Caffeine Refresh Token")
    private String caffeine;

    @Option(names = { "-d", "--debug"
    }, description = "Enables debug logging")
    private boolean debug = false;

    @Option(names = { "-L", "--log-load"
    }, description = "Enables CPU/RAM load logging")
    private boolean logLoad = false;

    @Option(names = { "-P", "--proxy"
    }, description = "The proxy address to use, no proxy is used by default")
    private String proxy;

    @Option(names = { "-H", "--host"
    }, description = "The address to bind to")
    private String host = "127.0.0.1";

    @Option(names = { "-p", "--port"
    }, description = "The port to listen on")
    private int port = 8080;

    public static void main(String[] args) {
        new CommandLine(new Launcher()).execute(args);
    }

    @Override
    public void run() {
        if (this.debug) {
            FastLoggingFramework.setDefautLevel(LogLevel.DEBUG);
            new FastLogger().debug("Debug mode enabled.");
        }

        if (this.proxy != null) {
            String[] proxy = this.proxy.split(":");
            int proxyPort = (proxy.length > 0) ? Integer.parseInt(proxy[1]) : 9050;

            WebUtil.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy[0], proxyPort)));
        }

        Koi koi = new Koi(this.host, this.port, this.debug, new CaffeineAuth(this.caffeine));

        koi.start();

        if (this.logLoad) {
            new RepeatingThread(TimeUnit.SECONDS.toMillis(10), () -> {
                long ram = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024) / 1024;

                Koi.getInstance().getLogger().info(String.format("RAM Usage: %dmb", ram));
            }).start();
        }
    }

}
