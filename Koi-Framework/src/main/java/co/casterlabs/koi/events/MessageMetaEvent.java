package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@NonNull
@ToString
@EqualsAndHashCode(callSuper = false)
public class MessageMetaEvent extends Event {
    private User streamer;
    private String id;

    public MessageMetaEvent(@NonNull User streamer, @NonNull String id) {
        this.streamer = streamer;
        this.id = id;
    }

    @SerializedName("is_visible")
    private @Setter boolean visible = true;
    private @Setter int upvotes = 0;

    @Override
    public EventType getType() {
        return EventType.META;
    }

}
