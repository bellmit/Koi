package co.casterlabs.koi.user.caffeine;

import java.util.ArrayList;
import java.util.List;

import co.casterlabs.caffeineapi.realtime.viewers.CaffeineViewers;
import co.casterlabs.caffeineapi.realtime.viewers.CaffeineViewersListener;
import co.casterlabs.caffeineapi.realtime.viewers.Viewer;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerLeaveEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@NonNull
@AllArgsConstructor
public class CaffeineViewersListenerAdapter implements CaffeineViewersListener {
    private CaffeineViewers conn;
    private ConnectionHolder holder;

    @Override
    public void onJoin(Viewer viewer) {
        this.holder.broadcastEvent(new ViewerJoinEvent(convertViewer(viewer), this.holder.getProfile()));
    }

    @Override
    public void onLeave(Viewer viewer) {
        this.holder.broadcastEvent(new ViewerLeaveEvent(convertViewer(viewer), this.holder.getProfile()));
    }

    @Override
    public void onViewerlist(List<Viewer> viewers) {
        List<User> list = new ArrayList<>();

        for (Viewer viewer : viewers) {
            list.add(convertViewer(viewer));
        }

        ViewerListEvent event = new ViewerListEvent(list, this.holder.getProfile());

        this.holder.setHeldEvent(event);
        this.holder.broadcastEvent(event);
    }

    private static User convertViewer(Viewer viewer) {
        try {
            if (viewer.isAnonymous()) {
                return EventType.getAnonymousUser();
            } else {
                return CaffeineUserConverter.getInstance().transform(viewer.getAsUser());
            }
        } catch (NullPointerException ignored) {
            return null;
        } // ???
    }

    @Override
    public void onAnonymousCount(int count) {} // Unused

    @Override
    public void onTotalCount(int count) {} // Unused

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed viewers for %s", this.holder.getSimpleProfile());
        }
    }

}
