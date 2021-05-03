package co.casterlabs.koi.client;

import co.casterlabs.koi.user.UserPlatform;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimpleProfile {
    private String id;
    private String channelId;
    private UserPlatform platform;

    @Override
    public String toString() {
        return String.format("%s/%s;%s", this.id, this.channelId, this.platform);
    }

}
