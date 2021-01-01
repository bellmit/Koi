package co.casterlabs.koi.networking.incoming;

import co.casterlabs.koi.events.EventType;
import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class TestEventRequest extends AbstractEvent<RequestType> {

    public TestEventRequest() {
        super(RequestType.TEST);
    }

    private EventType eventType;

}
