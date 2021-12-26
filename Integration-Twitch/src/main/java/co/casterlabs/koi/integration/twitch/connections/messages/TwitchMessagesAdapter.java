package co.casterlabs.koi.integration.twitch.connections.messages;

import java.io.IOException;

import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.integration.twitch.TwitchIntegration;
import co.casterlabs.koi.integration.twitch.impl.TwitchTokenAuth;
import lombok.NonNull;

public class TwitchMessagesAdapter implements Connection {
    private TwitchMessagesUser userMessages;
    private TwitchMessagesReceiver receiver;

    public TwitchMessagesAdapter(ConnectionHolder holder, @NonNull TwitchTokenAuth auth) {
        this.userMessages = new TwitchMessagesUser(holder, auth);
        this.receiver = new TwitchMessagesReceiver(holder, TwitchIntegration.getInstance().getAppAuth());
    }

    // If the command starts with '/' we need to send it as Casterlabs (The
    // receiver), otherwise we need to send it as the user (since it's a chat
    // message)
    //
    // This is to get around some nasty Twitch bugs where:
    // a) You don't receive message ids for your own message.
    // b) You can't actually send commands as the broadcaster.
    public void sendMessage(@NonNull String message) {
        if (message.startsWith("/") && !message.startsWith("/me")) {
            this.receiver.sendMessage(message);
        } else {
            this.userMessages.sendMessage(message);
        }
    }

    public void checkIfMod() {
        this.receiver.checkIfMod();
    }

    @Override
    public void close() throws IOException {
        Exception a = null;
        Exception b = null;

        try {
            this.userMessages.close();
        } catch (Exception e) {
            a = e;
        }

        try {
            this.receiver.close();
        } catch (Exception e) {
            b = e;
        }

        if (a != null) {
            throw new IOException(a);
        } else if (b != null) {
            throw new IOException(b);
        }
    }

    @Override
    public void open() throws IOException {
        if (!this.userMessages.isOpen()) {
            this.userMessages.open();
        }

        if (!this.receiver.isOpen()) {
            this.receiver.open();
        }

        this.checkIfMod();
    }

    @Override
    public boolean isOpen() {
        return this.userMessages.isOpen() && this.receiver.isOpen();
    }

}
