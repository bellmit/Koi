package co.casterlabs.koi.client;

import co.casterlabs.koi.user.UserPlatform;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimpleProfile {
    private String UUID;
    private UserPlatform platform;

    @Override
    public String toString() {
        return this.UUID + ";" + this.platform;
    }

}
