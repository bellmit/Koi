package co.casterlabs.koi.integration.glimesh.user;

import java.io.Closeable;
import java.io.IOException;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.realtime.GlimeshChatListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeChat;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChatMessage;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.integration.glimesh.GlimeshIntegration;
import co.casterlabs.koi.user.User;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class GlimeshChatWrapper implements Closeable, GlimeshChatListener {
    private GlimeshRealtimeChat conn;
    private ConnectionHolder holder;

    public GlimeshChatWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(holder.getProfile().getUsername());

        this.holder = holder;
        this.conn = new GlimeshRealtimeChat(GlimeshIntegration.getInstance().getAppAuth(), channel);

        this.conn.setListener(this);
        this.conn.connect();
    }

    @Override
    public void onChat(GlimeshChatMessage chat) {
        User sender = GlimeshUserConverter.getInstance().transform(chat.getUser());

        ChatEvent event = new ChatEvent(chat.getId(), chat.getMessage(), sender, this.holder.getProfile());

        this.holder.broadcastEvent(event);
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
        this.conn.close();
    }

}
