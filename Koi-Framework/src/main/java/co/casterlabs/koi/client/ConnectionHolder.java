package co.casterlabs.koi.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public class ConnectionHolder extends Cachable {
    public static final long DEAD_TIME = TimeUnit.SECONDS.toMillis(30);

    private @Getter(AccessLevel.NONE) String key;
    private @Getter @Setter Connection conn;

    private Set<Client> clients = new HashSet<>();
    private @Getter SimpleProfile simpleProfile;
    private User profile;

    private @Getter boolean expired = false;
    private FastLogger logger;

    private @Getter @Setter @NonNull List<ChatEvent> heldCatchupEvents = new LinkedList<>();
    private @Getter @Setter @Nullable Event heldEvent;

    public ConnectionHolder(@NonNull String key, @NonNull User profile) {
        super(key.isEmpty() ? Long.MAX_VALUE : DEAD_TIME); // Make phantom holders never say they've expired.

        this.key = key;
        this.profile = profile;
        this.simpleProfile = this.profile.getSimpleProfile();

        this.logger = new FastLogger(this.key);

        this.logger.debug("Created connection");
    }

    public void broadcastEvent(Event e) {
        for (Client client : new ArrayList<>(this.clients)) {
            client.broadcastEvent(e);
        }
    }

    public void updateProfile(User profile) {
        for (Client client : new ArrayList<>(this.clients)) {
            client.setProfile(profile);
        }
    }

    public User getProfile() {
        if (!this.clients.isEmpty()) {
            User heldProfile = this.clients.iterator().next().getProfile();

            if (heldProfile != null) {
                this.profile = heldProfile;
            }
        }

        return this.profile;
    }

    @Override
    public boolean onDispose(DisposeReason reason) {
        if (this.clients.size() > 0) {
            this.life += DEAD_TIME;

            return false;
        } else {
            this.expired = true;

            try {
                this.conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.logger.debug("Removed self from connection cache.");

            return true;
        }
    }

    @SneakyThrows
    public void addClient(Client client) {
        this.clients.add(client);

        if (!this.conn.isOpen()) {
            this.conn.open();
        }
    }

    @SneakyThrows
    public void removeClient(Client client) {
        this.clients.remove(client);
    }

}
