package co.casterlabs.koi.user.brime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import co.casterlabs.brimeapijava.realtime.BrimeRealtime;
import co.casterlabs.brimeapijava.realtime.BrimeRealtimeListener;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerLeaveEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.user.User;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@RequiredArgsConstructor
public class BrimeRealtimeAdapter implements BrimeRealtimeListener {
    private final ConnectionHolder holder;
    private final BrimeRealtime conn;

    private List<String> viewers = new LinkedList<>();
    private Set<String> oldViewersSet = new HashSet<>();

    @Override
    public void onChat(String username, String color, String message) {
        User sender = BrimeUserConverter.getInstance().get(username, color);

        ChatEvent e = new ChatEvent("-1", message, sender, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onFollow(String username, String id) {
        User follower = BrimeUserConverter.getInstance().get(username);

        FollowEvent e = new FollowEvent(follower, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onJoin(String username) {
        this.viewers.add(username);

        this.updateViewers();
    }

    @Override
    public void onLeave(String username) {
        this.viewers.remove(username);

        this.updateViewers();
    }

    private synchronized void updateViewers() {
        Set<String> set = new HashSet<>(this.viewers);
        List<User> viewers = new ArrayList<>();

        for (String username : set) {
            User viewer = BrimeUserConverter.getInstance().get(username);

            viewers.add(viewer);

            // JOIN
            if (!this.oldViewersSet.contains(username)) {
                this.holder.broadcastEvent(new ViewerJoinEvent(viewer, this.holder.getProfile()));
            }
        }

        for (String username : this.oldViewersSet) {
            // LEAVE
            if (!set.contains(username)) {
                User viewer = BrimeUserConverter.getInstance().get(username);

                this.holder.broadcastEvent(new ViewerLeaveEvent(viewer, this.holder.getProfile()));
            }
        }

        this.oldViewersSet = set;

        ViewerListEvent event = new ViewerListEvent(viewers, this.holder.getProfile());

        this.holder.setHeldEvent(event);
        this.holder.broadcastEvent(event);
    }

    @Override
    public void onClose() {
        this.viewers.clear();
        this.oldViewersSet.clear();

        if (!this.holder.isExpired()) {
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed chat for %s", this.holder.getSimpleProfile());
        }
    }

}
