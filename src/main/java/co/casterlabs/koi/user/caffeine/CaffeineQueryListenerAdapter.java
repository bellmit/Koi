package co.casterlabs.koi.user.caffeine;

import java.time.Instant;

import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQueryListener;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@NonNull
@RequiredArgsConstructor
public class CaffeineQueryListenerAdapter implements CaffeineQueryListener {
    private @NonNull CaffeineQuery conn;
    private @NonNull ConnectionHolder holder;

    private Instant streamStartedAt;

    @Override
    public void onStreamStateChanged(boolean isLive, String title) {
        if (isLive) {
            if (this.streamStartedAt == null) {
                this.streamStartedAt = Instant.now();
            }
        } else {
            this.streamStartedAt = null;
        }

        StreamStatusEvent e = new StreamStatusEvent(isLive, title, this.holder.getProfile(), this.streamStartedAt);

        this.holder.broadcastEvent(e);
        this.holder.setHeldEvent(e);
    }

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed query for %s", this.holder.getSimpleProfile());
        }
    }

}
