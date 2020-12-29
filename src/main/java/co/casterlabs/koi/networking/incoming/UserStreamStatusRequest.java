package co.casterlabs.koi.networking.incoming;

import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class UserStreamStatusRequest extends AbstractEvent<RequestType> {

    public UserStreamStatusRequest() {
        super(RequestType.USER_STREAM_STATUS);
    }

    private UserPlatform platform;
    private String username;

}
