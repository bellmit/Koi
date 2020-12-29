package co.casterlabs.koi.user;

import lombok.NonNull;

public interface UserProvider {

    public void hookWithAuth(@NonNull User user, @NonNull KoiAuthProvider auth) throws IdentifierException;

    public void hook(@NonNull User user, @NonNull String username) throws IdentifierException;

}
