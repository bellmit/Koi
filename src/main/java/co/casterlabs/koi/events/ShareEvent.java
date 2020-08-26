package co.casterlabs.koi.events;

import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;

// Dummy class, Share Event support was removed in 1.10
public class ShareEvent extends ChatEvent {

    public ShareEvent(String id, String message, SerializedUser sender, User streamer) {
        super(id, message, sender, streamer);
    }

}
