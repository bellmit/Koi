package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class ChatRequest extends AbstractEvent<RequestType> {

    public ChatRequest() {
        super(RequestType.CHAT);
    }

    private String message;
    private String nonce;

}
