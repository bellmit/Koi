package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MessageMetaEvent extends Event {
    private User streamer;
    private String id;

    private int upvotes;
    @SerializedName("is_visible")
    private boolean visible;

    @Override
    public EventType getType() {
        return EventType.META;
    }

}
