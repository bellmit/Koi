package co.casterlabs.koi.user;

public interface UserProvider {
    public User get(String identifier, Object data) throws IdentifierException;

}
