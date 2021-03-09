package co.casterlabs.koi.user.twitch;

import java.io.Closeable;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;

import co.casterlabs.koi.client.Puppet;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchPuppetMessages implements TwirkListener, Closeable {
    private Twirk twirk;
    private TwitchTokenAuth auth;
    private Puppet puppet;

    public TwitchPuppetMessages(@NonNull Puppet puppet, @NonNull TwitchTokenAuth auth) {
        this.puppet = puppet;
        this.auth = auth;

        this.reconnect();
    }

    public void sendMessage(@NonNull String message) {
        this.twirk.channelMessage(message);
    }

    private void reconnect() {
        try {
            this.twirk = this.auth.getTwirk(this.puppet.getClient().getProfile().getUsername());

            this.twirk.addIrcListener(this);
            this.twirk.connect();
        } catch (Exception e) {
            this.reconnect();
        }
    }

    @Override
    public void onDisconnect() {
        if (this.puppet.isExpired()) {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed messages for %s", this.puppet.getClient().getSimpleProfile());
        } else {
            this.reconnect();
        }
    }

    @Override
    public void close() {
        this.twirk.close();
    }

}
