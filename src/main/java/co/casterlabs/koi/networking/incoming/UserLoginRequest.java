package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class UserLoginRequest extends AbstractEvent<RequestType> {

    public UserLoginRequest() {
        super(RequestType.LOGIN);
    }

    private String nonce;
    private String token;

}
