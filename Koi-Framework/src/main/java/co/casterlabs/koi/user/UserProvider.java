package co.casterlabs.koi.user;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.Puppet;
import lombok.NonNull;

public interface UserProvider {

    public void hookWithAuth(@NonNull Client client, @NonNull ClientAuthProvider auth) throws IdentifierException;

    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException;

    default void upvote(@NonNull Client client, @NonNull String id, @NonNull ClientAuthProvider auth) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    default void chat(@NonNull Client client, @NonNull String message, @NonNull ClientAuthProvider auth) throws UnsupportedOperationException, ApiAuthException {
        throw new UnsupportedOperationException();
    }

    default void deleteMessage(@NonNull Client client, @NonNull String messageId, @NonNull ClientAuthProvider auth) throws UnsupportedOperationException, ApiAuthException {
        throw new UnsupportedOperationException();
    }

    // For Twitch
    default void initializePuppet(@NonNull Puppet puppet) throws ApiAuthException {}

    // For Twitch
    default void chatAsPuppet(@NonNull Puppet puppet, @NonNull String message) throws UnsupportedOperationException, ApiAuthException {
        this.chat(puppet.getClient(), message, puppet.getAuth());
    }

}
