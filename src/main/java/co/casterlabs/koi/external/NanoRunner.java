package co.casterlabs.koi.external;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD.AsyncRunner;
import fi.iki.elonen.NanoHTTPD.ClientHandler;

public class NanoRunner implements AsyncRunner {
    private ThreadPoolExecutor threadpool = new ThreadPoolExecutor(2, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    public void closeAll() {
        this.threadpool.shutdownNow();
    }

    @Override
    public void closed(ClientHandler clientHandler) {}

    @Override
    public void exec(ClientHandler clientHandler) {
        try {
            this.threadpool.invokeAll(Collections.singleton(() -> {
                clientHandler.run();
                return null;
            }), 15, TimeUnit.SECONDS); // Kill it after 15s, to prevent thread leaks.
        } catch (Exception ignored) {}
    }

}
