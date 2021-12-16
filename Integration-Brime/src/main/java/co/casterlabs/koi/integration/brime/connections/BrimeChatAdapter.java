package co.casterlabs.koi.integration.brime.connections;

import java.io.IOException;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.brimeapijava.realtime.BrimeChat;
import co.casterlabs.brimeapijava.realtime.BrimeChatListener;
import co.casterlabs.brimeapijava.realtime.BrimeChatMessage;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.integration.brime.data.BrimeUserConverter;
import co.casterlabs.koi.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@RequiredArgsConstructor
public class BrimeChatAdapter implements BrimeChatListener, Connection {
    private final ConnectionHolder holder;
    private final @Getter BrimeChat conn;

    private void holdChatEvent(ChatEvent e) {
        this.holder.getHeldCatchupEvents().add(e);

        // Shift the list over, keeps it capped at 100 message history.
        if (this.holder.getHeldCatchupEvents().size() > 100) {
            this.holder.getHeldCatchupEvents().remove(0);
        }
    }

    @Override
    public void onChat(BrimeChatMessage chat) {
        User sender = BrimeUserConverter.getInstance().transform(chat.getSender());

        // Temp
        String message = chat.getContent().getRaw();
        if (chat.getReply() != null) {
            message = String.format("@%s %s", chat.getReply().getSender().getDisplayname(), chat.getContent().getRaw());
        }

        ChatEvent e = new ChatEvent(chat.getMessageId(), message, sender, this.holder.getProfile());

        e.abilities.setDeletable(true);

//        for (Entry<String, BrimeChatEmote> entry : chat.getEmotes().entrySet()) {
//            e.getEmotes().put(entry.getKey(), entry.getValue().get3xImageUrl());
//        }

        this.holdChatEvent(e);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            Koi.clientThreadPool.submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed chat for %s", this.holder.getSimpleProfile());
        }
    }

    @Override
    public void close() throws IOException {
        this.conn.close();
    }

    @Override
    public void open() throws IOException {
        try {
            this.conn.connect();
        } catch (ApiAuthException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isOpen() {
        return this.conn.isOpen();
    }

}
