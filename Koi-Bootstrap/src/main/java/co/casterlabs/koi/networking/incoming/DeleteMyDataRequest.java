package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class DeleteMyDataRequest extends AbstractEvent<IncomingMessageType> {

    public DeleteMyDataRequest() {
        super(IncomingMessageType.DELETE_MY_DATA);
    }

    private String nonce;
    private String token;

}
