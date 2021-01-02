package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class CredentialsRequest extends AbstractEvent<RequestType> {

    public CredentialsRequest() {
        super(RequestType.CREDENTIALS);
    }

}
