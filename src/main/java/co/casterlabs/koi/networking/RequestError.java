package co.casterlabs.koi.networking;

public enum RequestError {
    SERVER_INTERNAL_ERROR,
    SERVER_API_ERROR,

    REQUEST_TYPE_INVAID,
    REQUEST_JSON_INVAID,
    REQUEST_CRITERIA_INVAID,

    USER_ID_INVALID,
    USER_LIMIT_REACHED,
    USER_NOT_PRESENT,

    ;
}
