package co.casterlabs.koi.external;

import java.io.FileInputStream;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class StatsEndpoint extends NanoHTTPD implements Server {

    public StatsEndpoint(int port) {
        super(port);

        this.setAsyncRunner(new NanoRunner("Stats Endpoint"));
    }

    @SneakyThrows
    @Override
    public Response serve(IHTTPSession session) {
        return NanoHTTPD.newChunkedResponse(Status.OK, "application/json", new FileInputStream("stats.json"));
    }

    @SneakyThrows
    @Override
    public void start() {
        super.start();

        FastLogger.logStatic("Stats Endpoint started on port %d!", this.getListeningPort());
    }

    @Override
    public boolean isRunning() {
        return this.isAlive();
    }

}
