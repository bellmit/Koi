package co.casterlabs.koi.integration.glimesh.connections;

import java.io.Closeable;
import java.io.IOException;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.realtime.GlimeshFollowerListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeFollowers;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.integration.glimesh.GlimeshIntegration;
import co.casterlabs.koi.integration.glimesh.data.GlimeshUserConverter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class GlimeshFollowerWrapper implements Closeable, GlimeshFollowerListener, Connection {
    private GlimeshRealtimeFollowers conn;
    private ConnectionHolder holder;

    public GlimeshFollowerWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannelByUserId(holder.getSimpleProfile().getId());

        this.holder = holder;
        this.conn = new GlimeshRealtimeFollowers(GlimeshIntegration.getInstance().getAppAuth(), channel.getStreamer());

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
            Koi.clientThreadPool.submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed followers for %s", this.holder.getSimpleProfile());
        }
    }

    @Override
    public void close() throws IOException {
        if (this.isOpen()) {
            this.conn.close();
        }
    }

    @Override
    public void open() throws IOException {
        if (!this.isOpen()) {
            this.conn.connect();
        }
    }

    @Override
    public boolean isOpen() {
        return this.conn.isOpen();
    }

}
