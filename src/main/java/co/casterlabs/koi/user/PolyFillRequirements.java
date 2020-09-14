package co.casterlabs.koi.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum PolyFillRequirements {
    COLOR;

    public static List<PolyFillRequirements> getPolyFillForPlatform(UserPlatform platform) {
        switch (platform) {
            case CAFFEINE:
                return Arrays.asList(COLOR);

            case TWITCH:
                return new ArrayList<>();

            default:
                return new ArrayList<>();

        }
    }

}
