package co.casterlabs.koi.external;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD.AsyncRunner;
import fi.iki.elonen.NanoHTTPD.ClientHandler;

public class NanoRunner implements AsyncRunner {
    private ThreadPoolExecutor threadpool = new ThreadPoolExecutor(1, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public NanoRunner(String name) {
        this.threadpool.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                Thread t = new Thread(run);

                t.setName(name + " Thread Pool - Koi #" + (threadpool.getActiveCount() + 1));

                return t;
            }
        });
    }

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
