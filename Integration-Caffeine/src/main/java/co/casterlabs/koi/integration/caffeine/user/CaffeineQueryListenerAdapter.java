package co.casterlabs.koi.integration.caffeine.user;

import java.io.IOException;
import java.time.Instant;

import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQueryListener;
import co.casterlabs.caffeineapi.types.CaffeineStage;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.Connection;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@NonNull
@RequiredArgsConstructor
public class CaffeineQueryListenerAdapter implements CaffeineQueryListener, Connection {
    private @NonNull CaffeineQuery conn;
    private @NonNull ConnectionHolder holder;

    private Instant streamStartedAt;

    @Override
    public void onStageUpdate(CaffeineStage stage) {
        if (stage.isLive()) {
            if (this.streamStartedAt == null) {
                this.streamStartedAt = Instant.now();
            }
        } else {
            this.streamStartedAt = null;
        }

        StreamStatusEvent e = new StreamStatusEvent(stage.isLive(), stage.getTitle(), this.holder.getProfile(), this.streamStartedAt);

        this.holder.broadcastEvent(e);
        this.holder.setHeldEvent(e);
    }

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            Koi.clientThreadPool.submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed query for %s", this.holder.getSimpleProfile());
        }
    }

    @Override
    public void close() throws IOException {
        this.conn.close();
    }

    @Override
    public void open() throws IOException {
        this.conn.connect();
    }

    @Override
    public boolean isOpen() {
        return this.conn.isOpen();
    }

}
