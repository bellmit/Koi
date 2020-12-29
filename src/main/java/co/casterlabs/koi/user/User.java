package co.casterlabs.koi.user;

import java.util.ArrayList;
import java.util.List;

import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.Natsukashii.AuthException;
import co.casterlabs.koi.events.Event;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class User {
    private UserListener listener;

    private List<ConnectionHolder> closables = new ArrayList<>();

    public User(@NonNull UserListener listener, @NonNull String token) throws IdentifierException {
        try {
            KoiAuthProvider auth = Natsukashii.get(token);

            this.listener = listener;

            auth.getPlatform().getProvider().hookWithAuth(this, auth);
        } catch (AuthException e) {
            throw new IdentifierException();
        }
    }

    public User(@NonNull UserListener listener, @NonNull String username, @NonNull UserPlatform platform) throws IdentifierException {
        this.listener = listener;

        platform.getProvider().hook(this, username);
    }

    public void broadcastEvent(@NonNull Event e) {
        this.listener.onEvent(e);
    }

    public void close() {
        for (ConnectionHolder closeable : this.closables) {
            closeable.getUsers().remove(this);
        }
    }

    public void updateProfileSafe(SerializedUser profile) {
        for (ConnectionHolder holder : this.closables) {
            holder.setProfile(profile);
        }
    }

}
