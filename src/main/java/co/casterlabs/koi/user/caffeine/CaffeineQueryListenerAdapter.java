package co.casterlabs.koi.user.caffeine;

import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQueryListener;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.user.ConnectionHolder;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@AllArgsConstructor
public class CaffeineQueryListenerAdapter implements CaffeineQueryListener {
    private @NonNull CaffeineQuery conn;
    private @NonNull ConnectionHolder holder;

    @Override
    public void onStreamStateChanged(boolean isLive, String title) {
        StreamStatusEvent e = new StreamStatusEvent(isLive, title, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onClose(boolean remote) {
        if (remote) {
            this.conn.connect();
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed query for %s;%s", this.holder.getProfile().getUUID(), this.holder.getProfile().getPlatform());
        }
    }

}
