package co.casterlabs.koi.integration.twitch.connections.messages;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;

import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerLeaveEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
import co.casterlabs.koi.integration.twitch.impl.TwitchTokenAuth;
import co.casterlabs.koi.user.User;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchMessagesUser implements TwirkListener, Connection {
    private Twirk twirk;
    private ConnectionHolder holder;
    private TwitchTokenAuth auth;

    private Map<String, User> viewers = new HashMap<>();

    public TwitchMessagesUser(ConnectionHolder holder, @NonNull TwitchTokenAuth auth) {
        this.holder = holder;
        this.auth = auth;
    }

    public void sendMessage(@NonNull String message) {
        this.twirk.channelMessage(message);
    }

    private void reconnect() {
        try {
            this.twirk = this.auth.getTwirk(this.holder.getProfile().getUsername());

            this.viewers.clear();
            this.holder.setHeldEvent(null);
            this.twirk.addIrcListener(this);
            this.twirk.connect();
        } catch (Exception e) {
            this.reconnect();
        }
    }

    @Override
    public void onDisconnect() {
        if (this.holder.isExpired()) {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed messages for %s", this.holder.getSimpleProfile());
        } else {
            this.reconnect();
        }
    }

    @Override
    public void onJoin(String name) {
        User user = TwitchUserConverter.getInstance().get(name);

        this.viewers.put(name, user);
        this.holder.broadcastEvent(new ViewerJoinEvent(user, this.holder.getProfile()));

        this.updateViewers();
    }

    @Override
    public void onPart(String name) {
        User user = TwitchUserConverter.getInstance().get(name);

        this.viewers.remove(name);
        this.holder.broadcastEvent(new ViewerLeaveEvent(user, this.holder.getProfile()));

        this.updateViewers();
    }

    @Override
    public void onNamesList(Collection<String> names) {
        for (String name : names) {
            this.viewers.put(name, TwitchUserConverter.getInstance().get(name));
        }

        this.updateViewers();
    }

    private void updateViewers() {
        ViewerListEvent event = new ViewerListEvent(this.viewers.values(), this.holder.getProfile());

        this.holder.setHeldEvent(event);
        this.holder.broadcastEvent(event);
    }

    @Override
    public void close() {
        this.twirk.close();
    }

    @Override
    public void open() throws IOException {
        this.reconnect();
    }

    @Override
    public boolean isOpen() {
        if (this.twirk == null) {
            return false;
        } else {
            return this.twirk.isConnected();
        }
    }

}
