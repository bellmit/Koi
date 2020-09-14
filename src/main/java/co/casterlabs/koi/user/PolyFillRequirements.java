package co.casterlabs.koi.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum PolyFillRequirements {
    COLOR,
    PROFILE_PICTURE;

    public static List<PolyFillRequirements> getPolyFillForPlatform(UserPlatform platform) {
        switch (platform) {
            case CAFFEINE:
                return Arrays.asList(COLOR);

            case TWITCH:
                return Arrays.asList(PROFILE_PICTURE);

            default:
                return new ArrayList<>();

        }
    }

}
