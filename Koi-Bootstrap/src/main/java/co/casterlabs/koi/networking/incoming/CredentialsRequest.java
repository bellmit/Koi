package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class CredentialsRequest extends AbstractEvent<IncomingMessageType> {

    public CredentialsRequest() {
        super(IncomingMessageType.CREDENTIALS);
    }

    private String nonce;

}
