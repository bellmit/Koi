package co.casterlabs.koi.user;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public interface UserConverter<T> {

    public @NonNull User transform(@NonNull T object);

    public @Nullable User get(@NonNull String username);

}
