package co.casterlabs.koi.user;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.Natsukashii.AuthException;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class Client {
    private List<ConnectionHolder> connections = new ArrayList<>();
    private ClientEventListener listener;

    private @Getter(AccessLevel.NONE) String token;
    private KoiAuthProvider auth;
    private @Setter String UUID;
    private @Setter String username;

    private RepeatingThread authValidator = new RepeatingThread("Client auth validator", TimeUnit.MINUTES.toMillis(5), () -> {
        if (!this.auth.isValid()) {
            this.notifyCredentialExpired();
        }
    });

    public Client(@NonNull ClientEventListener listener, @NonNull String token) throws IdentifierException, PlatformException {
        try {
            this.auth = Natsukashii.get(token);

            if (this.auth.getPlatform().isEnabled()) {
                this.listener = listener;
                this.token = token;

                this.authValidator.start();

                this.auth.getPlatform().getProvider().hookWithAuth(this, this.auth);

                for (ConnectionHolder holder : this.connections) {
                    if (holder.getHeldEvent() != null) {
                        if (holder.getHeldEvent() instanceof ViewerListEvent) {
                            ViewerListEvent viewerListEvent = (ViewerListEvent) holder.getHeldEvent();

                            for (User viewer : viewerListEvent.getViewers()) {
                                this.broadcastEvent(new ViewerJoinEvent(viewer, holder.getProfile()));
                            }
                        }

                        this.broadcastEvent(holder.getHeldEvent());
                    }
                }
            } else {
                throw new PlatformException();
            }
        } catch (AuthException e) {
            throw new IdentifierException();
        }
    }

    public Client(@NonNull ClientEventListener listener, @NonNull String username, @NonNull UserPlatform platform) throws IdentifierException, PlatformException {
        if (platform.isEnabled()) {
            this.listener = listener;

            platform.getProvider().hook(this, username);
        } else {
            throw new PlatformException();
        }
    }

    public void broadcastEvent(@NonNull Event e) {
        this.listener.onEvent(e);
    }

    public void upvote(@NonNull String id) throws UnsupportedOperationException {
        this.auth.getPlatform().getProvider().upvote(this, id, this.auth);
    }

    public void chat(@NonNull String message) {
        this.auth.getPlatform().getProvider().chat(this, message, this.auth);
    }

    public JsonElement getCredentials() {
        if (this.auth != null) {
            if (this.auth.isValid()) {
                return this.auth.getCredentials();
            } else {
                this.notifyCredentialExpired();
            }
        }

        return JsonNull.INSTANCE;
    }

    public void notifyCredentialExpired() {
        if (this.token != null) {
            Natsukashii.revoke(this.token);
        }

        this.listener.onCredentialExpired();
    }

    public void close() {
        this.authValidator.stop();

        for (ConnectionHolder closeable : this.connections) {
            closeable.getClients().remove(this);
        }
    }

    public void updateProfileSafe(User profile) {
        for (ConnectionHolder holder : this.connections) {
            holder.setProfile(profile);
        }
    }

}
