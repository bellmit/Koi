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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class Client {
    private UserListener listener;

    private List<ConnectionHolder> connections = new ArrayList<>();

    private @Getter(AccessLevel.NONE) String token;
    private KoiAuthProvider auth;

    private RepeatingThread authValidator = new RepeatingThread("Client auth validator", TimeUnit.MINUTES.toMillis(5), () -> {
        if (!this.auth.isValid()) {
            this.onCredentialExpired();
        }
    });

    public Client(@NonNull UserListener listener, @NonNull String token) throws IdentifierException {
        try {
            this.auth = Natsukashii.get(token);

            this.listener = listener;
            this.token = token;

            this.authValidator.start();

            this.auth.getPlatform().getProvider().hookWithAuth(this, this.auth);
        } catch (AuthException e) {
            throw new IdentifierException();
        }
    }

    public Client(@NonNull UserListener listener, @NonNull String username, @NonNull UserPlatform platform) throws IdentifierException {
        this.listener = listener;

        platform.getProvider().hook(this, username);
    }

    public void broadcastEvent(@NonNull Event e) {
        this.listener.onEvent(e);
    }

    public JsonElement getCredentials() {
        if (this.auth != null) {
            if (this.auth.isValid()) {
                return this.auth.getCredentials();
            } else {
                this.onCredentialExpired();
            }
        }

        return JsonNull.INSTANCE;
    }

    public void onCredentialExpired() {
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
