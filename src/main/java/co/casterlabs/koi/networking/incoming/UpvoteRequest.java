package co.casterlabs.koi.networking.incoming;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.events.EventType;
import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class UpvoteRequest extends AbstractEvent<RequestType> {

    public UpvoteRequest() {
        super(RequestType.UPVOTE);
    }

    @SerializedName("message_id")
    private String messageId;

    @SerializedName("message_type")
    private EventType messageType;

}
