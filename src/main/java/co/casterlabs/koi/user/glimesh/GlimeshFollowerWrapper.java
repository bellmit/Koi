package co.casterlabs.koi.user.glimesh;

import java.io.Closeable;
import java.io.IOException;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.realtime.GlimeshFollowerListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeFollowers;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.user.UserPlatform;

public class GlimeshFollowerWrapper implements Closeable, GlimeshFollowerListener {
    private GlimeshRealtimeFollowers conn;
    private ConnectionHolder holder;

    public GlimeshFollowerWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(holder.getProfile().getUsername());

        this.holder = holder;
        this.conn = new GlimeshRealtimeFollowers((GlimeshAuth) Koi.getInstance().getAuthProvider(UserPlatform.GLIMESH), channel.getStreamer());

        this.conn.setListener(this);
        this.conn.connect();
    }

    @Override
    public void onFollow(GlimeshUser follower) {
        FollowEvent event = new FollowEvent(GlimeshUserConverter.getInstance().transform(follower), this.holder.getProfile());

        this.holder.broadcastEvent(event);
    }

    public void onClose() {
        if (!this.holder.isExpired()) {
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        }
    }

    @Override
    public void close() throws IOException {
        this.conn.close();
    }

}
