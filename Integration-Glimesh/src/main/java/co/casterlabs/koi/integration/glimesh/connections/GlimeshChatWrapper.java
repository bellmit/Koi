package co.casterlabs.koi.integration.glimesh.connections;

import java.io.Closeable;
import java.io.IOException;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.realtime.GlimeshChatListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeChat;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChatMessage;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.integration.glimesh.GlimeshIntegration;
import co.casterlabs.koi.integration.glimesh.data.GlimeshUserConverter;
import co.casterlabs.koi.user.User;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class GlimeshChatWrapper implements Closeable, GlimeshChatListener, Connection {
    private GlimeshRealtimeChat conn;
    private ConnectionHolder holder;

    public GlimeshChatWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(holder.getSimpleProfile().getChannelId());

        this.holder = holder;
        this.conn = new GlimeshRealtimeChat(GlimeshIntegration.getInstance().getAppAuth(), channel);

        this.conn.setListener(this);
        this.conn.connect();
    }

    private void holdChatEvent(ChatEvent e) {
        this.holder.getHeldCatchupEvents().add(e);

        // Shift the list over, keeps it capped at 100 message history.
        if (this.holder.getHeldCatchupEvents().size() > 100) {
            this.holder.getHeldCatchupEvents().remove(0);
        }
    }

    @Override
    public void onChat(GlimeshChatMessage chat) {
        User sender = GlimeshUserConverter.getInstance().transform(chat.getUser());

        ChatEvent e = new ChatEvent(chat.getId(), chat.getMessage(), sender, this.holder.getProfile());

        this.holdChatEvent(e);

        this.holder.broadcastEvent(e);
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
