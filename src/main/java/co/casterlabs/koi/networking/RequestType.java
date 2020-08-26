package co.casterlabs.koi.networking;

public enum RequestType {
    ADD,
    REMOVE,
    CLOSE,
    TEST,
    PREFERENCES,
    KEEP_ALIVE,
    UNKNOWN;

    public static RequestType fromString(String type) {
        for (RequestType platform : RequestType.values()) {
            if (platform.name().equalsIgnoreCase(type)) {
                return platform;
            }
        }

        return UNKNOWN;
    }

}
