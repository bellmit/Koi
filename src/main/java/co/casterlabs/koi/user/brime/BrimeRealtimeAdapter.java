package co.casterlabs.koi.user.brime;

import co.casterlabs.brimeapijava.realtime.BrimeRealtime;
import co.casterlabs.brimeapijava.realtime.BrimeRealtimeListener;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@AllArgsConstructor
public class BrimeRealtimeAdapter implements BrimeRealtimeListener {
    private ConnectionHolder holder;
    private BrimeRealtime conn;

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
    public void onClose() {
        if (!this.holder.isExpired()) {
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed chat for %s", this.holder.getSimpleProfile());
        }
    }

}
