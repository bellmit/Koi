package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class UserLoginRequest extends AbstractEvent<IncomingMessageType> {

    public UserLoginRequest() {
        super(IncomingMessageType.LOGIN);
    }

    private String nonce;
    private String token;

}
