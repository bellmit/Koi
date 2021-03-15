package co.casterlabs.koi.user.glimesh;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.realtime.GlimeshChannelListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChannel.ChannelStatus;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.user.UserPlatform;

public class GlimeshStreamWrapper implements Closeable, GlimeshChannelListener {
    private GlimeshRealtimeChannel conn;
    private ConnectionHolder holder;

    private Instant streamStartedAt;

    public GlimeshStreamWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(holder.getProfile().getUsername());

        this.holder = holder;
        this.conn = new GlimeshRealtimeChannel((GlimeshAuth) Koi.getInstance().getAuthProvider(UserPlatform.GLIMESH), channel.getId());

        this.onUpdate(channel);

        this.conn.setListener(this);
        this.conn.connect();
    }

    @Override
    public void onUpdate(GlimeshChannel channel) {
        boolean isLive = channel.getStatus() == ChannelStatus.LIVE;

        if (isLive) {
            if (this.streamStartedAt == null) {
                this.streamStartedAt = Instant.now();
            }
        } else {
            this.streamStartedAt = null;
        }

        StreamStatusEvent e = new StreamStatusEvent(isLive, channel.getTitle(), GlimeshUserConverter.getInstance().transform(channel.getStreamer()), this.streamStartedAt);

        this.holder.broadcastEvent(e);
        this.holder.setHeldEvent(e);
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
