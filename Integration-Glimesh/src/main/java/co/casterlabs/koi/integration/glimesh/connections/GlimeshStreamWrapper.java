package co.casterlabs.koi.integration.glimesh.connections;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.realtime.GlimeshChannelListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChannel.ChannelStatus;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.integration.glimesh.GlimeshIntegration;
import co.casterlabs.koi.integration.glimesh.data.GlimeshUserConverter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class GlimeshStreamWrapper implements Closeable, GlimeshChannelListener, Connection {
    private GlimeshRealtimeChannel conn;
    private ConnectionHolder holder;

    private Instant streamStartedAt;

    public GlimeshStreamWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(holder.getSimpleProfile().getChannelId());

        this.holder = holder;
        this.conn = new GlimeshRealtimeChannel(GlimeshIntegration.getInstance().getAppAuth(), channel);

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
            Koi.clientThreadPool.submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed query for %s", this.holder.getSimpleProfile());
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
