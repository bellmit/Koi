package co.casterlabs.koi.user;

public interface UserConverter<T> {

    public SerializedUser transform(T object);

    public SerializedUser get(String UUID) throws IdentifierException;

}
