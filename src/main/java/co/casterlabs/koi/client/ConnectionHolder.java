package co.casterlabs.koi.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
    private @Getter(AccessLevel.NONE) String key;
    private @Setter Closeable closeable;

    private Set<Client> clients = new HashSet<>();
    private SimpleProfile simpleProfile;

    private boolean expired = false;
    private FastLogger logger;

    private @Setter @Nullable Event heldEvent;

    public ConnectionHolder(@NonNull String key, @NonNull SimpleProfile simpleProfile) {
        super(TimeUnit.MINUTES, 1);

        this.key = key;
        this.simpleProfile = simpleProfile;

        this.logger = new FastLogger(this.key);

        this.logger.debug("Created connection");
    }

    public void broadcastEvent(Event e) {
        for (Client user : new ArrayList<>(this.clients)) {
            user.broadcastEvent(e);
        }
    }

    public User getProfile() {
        if (this.clients.isEmpty()) {
            throw new IllegalStateException();
        } else {
            return this.clients.iterator().next().getProfile();
        }
    }

    public void updateProfile(User profile) {
        for (Client user : new ArrayList<>(this.clients)) {
            user.setProfile(profile);
        }
    }

    @Override
    public boolean onDispose(DisposeReason reason) {
        if (this.clients.size() > 0) {
            this.life += TimeUnit.MINUTES.toMillis(1);

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
