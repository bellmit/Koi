package co.casterlabs.koi.user;

import lombok.NonNull;

public interface UserProvider {

    public void hookWithAuth(@NonNull UserConnection user, @NonNull KoiAuthProvider auth) throws IdentifierException;

    public void hook(@NonNull UserConnection user, @NonNull String username) throws IdentifierException;

}
