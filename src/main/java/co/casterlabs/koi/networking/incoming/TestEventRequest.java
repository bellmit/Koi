package co.casterlabs.koi.networking.incoming;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.events.EventType;
import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class TestEventRequest extends AbstractEvent<RequestType> {

    public TestEventRequest() {
        super(RequestType.TEST);
    }

    @SerializedName("type")
    private EventType testType;

}
