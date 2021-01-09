package co.casterlabs.koi.user.caffeine;

import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQueryListener;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.user.ConnectionHolder;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@NonNull
@AllArgsConstructor
public class CaffeineQueryListenerAdapter implements CaffeineQueryListener {
    private CaffeineQuery conn;
    private ConnectionHolder holder;

    @Override
    public void onStreamStateChanged(boolean isLive, String title) {
        StreamStatusEvent e = new StreamStatusEvent(isLive, title, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed query for %s;%s", this.holder.getProfile().getUUID(), this.holder.getProfile().getPlatform());
        }
    }

}
