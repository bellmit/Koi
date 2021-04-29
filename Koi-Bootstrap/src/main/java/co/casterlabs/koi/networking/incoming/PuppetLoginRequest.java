package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class PuppetLoginRequest extends AbstractEvent<IncomingMessageType> {

    public PuppetLoginRequest() {
        super(IncomingMessageType.PUPPET_LOGIN);
    }

    private String nonce;
    private String token;

}
