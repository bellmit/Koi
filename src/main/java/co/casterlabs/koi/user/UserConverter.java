package co.casterlabs.koi.user;

import com.google.gson.JsonObject;

public interface UserConverter<T> {

    public JsonObject transform(T object);

    public JsonObject get(String UUID) throws IdentifierException;
    
}
