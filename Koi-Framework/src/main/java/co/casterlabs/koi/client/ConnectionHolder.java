package co.casterlabs.koi.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

@Getter
public class ConnectionHolder extends Cachable {
    public static final long DEAD_TIME = TimeUnit.SECONDS.toMillis(30);

    private @Getter(AccessLevel.NONE) String key;
    private @Setter Closeable closeable;

    private Set<Client> clients = new HashSet<>();
    private SimpleProfile simpleProfile;
    private User profile;

    private boolean expired = false;
    private FastLogger logger;

    private @Setter @NonNull List<Event> heldCatchupEvents = new LinkedList<>();
    private @Setter @Nullable Event heldEvent;

    public ConnectionHolder(@NonNull String key, @NonNull SimpleProfile simpleProfile) {
        super(key.isEmpty() ? Long.MAX_VALUE : DEAD_TIME); // Make phantom holders never say they've expired.

        this.key = key;
        this.simpleProfile = simpleProfile;

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
            this.profile = this.clients.iterator().next().getProfile();
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
                this.closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.logger.debug("Removed self from connection cache.");

            return true;
        }
    }

}
