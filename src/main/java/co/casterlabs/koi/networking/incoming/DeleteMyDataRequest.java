package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class DeleteMyDataRequest extends AbstractEvent<RequestType> {

    public DeleteMyDataRequest() {
        super(RequestType.DELETE_MY_DATA);
    }

    private String nonce;
    private String token;

}
