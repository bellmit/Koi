package co.casterlabs.koi.client;

import co.casterlabs.koi.user.UserPlatform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class SimpleProfile {
    private String UUID;
    private UserPlatform platform;

}
