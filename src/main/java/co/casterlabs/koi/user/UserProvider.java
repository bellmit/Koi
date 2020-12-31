package co.casterlabs.koi.user;

import lombok.NonNull;

public interface UserProvider {

    public void hookWithAuth(@NonNull Client user, @NonNull KoiAuthProvider auth) throws IdentifierException;

    public void hook(@NonNull Client user, @NonNull String username) throws IdentifierException;

}
