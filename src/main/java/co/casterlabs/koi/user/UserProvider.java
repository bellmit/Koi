package co.casterlabs.koi.user;

import co.casterlabs.koi.IdentifierException;

public interface UserProvider {
    public User get(String identifier, Object data) throws IdentifierException;

}
