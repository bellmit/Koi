package co.casterlabs.koi.networking.incoming;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class UpvoteRequest extends AbstractEvent<IncomingMessageType> {

    public UpvoteRequest() {
        super(IncomingMessageType.UPVOTE);
    }

    private String nonce;

    @SerializedName("message_id")
    private String messageId;

}
