package co.casterlabs.koi.user;

import co.casterlabs.koi.events.Event;

public interface ClientEventListener {
    public void onEvent(Event e);

    public void onCredentialExpired();

}
