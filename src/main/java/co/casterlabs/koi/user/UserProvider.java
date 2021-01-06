package co.casterlabs.koi.user;

import lombok.NonNull;

public interface UserProvider {

    public void hookWithAuth(@NonNull Client client, @NonNull KoiAuthProvider auth) throws IdentifierException;

    public void hook(@NonNull Client client, @NonNull String username) throws IdentifierException;

    public void upvote(@NonNull Client client, @NonNull String id, @NonNull KoiAuthProvider auth) throws UnsupportedOperationException;

    public void chat(Client client, @NonNull String message, KoiAuthProvider auth);

}
