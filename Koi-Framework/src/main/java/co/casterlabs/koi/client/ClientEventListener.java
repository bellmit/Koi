package co.casterlabs.koi.client;

import co.casterlabs.koi.events.Event;
import lombok.NonNull;

public interface ClientEventListener {

    public void onEvent(@NonNull Event e);

    public void onCredentialExpired();

}
