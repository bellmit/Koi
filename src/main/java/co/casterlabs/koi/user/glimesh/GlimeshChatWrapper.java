package co.casterlabs.koi.user.glimesh;

import java.io.Closeable;
import java.io.IOException;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.realtime.GlimeshChatListener;
import co.casterlabs.glimeshapijava.realtime.GlimeshRealtimeChat;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChatMessage;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;

public class GlimeshChatWrapper implements Closeable, GlimeshChatListener {
    private GlimeshRealtimeChat conn;
    private ConnectionHolder holder;

    public GlimeshChatWrapper(ConnectionHolder holder) throws NumberFormatException, ApiAuthException, ApiException {
        GlimeshChannel channel = GlimeshUserConverter.getInstance().getChannel(holder.getProfile().getUsername());

        this.holder = holder;
        this.conn = new GlimeshRealtimeChat((GlimeshAuth) Koi.getInstance().getAuthProvider(UserPlatform.GLIMESH), channel.getId());

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
            Koi.getClientThreadPool().submit(() -> this.conn.connect());
        }
    }

    @Override
    public void close() throws IOException {
        this.conn.close();
    }

}
