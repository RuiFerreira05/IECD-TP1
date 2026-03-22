package iecd.a51597.server.protocol.types;

public enum ErrorCodeType {
    AUTH_FAILED,
    USERNAME_TAKEN,
    SESSION_EXPIRED,
    NOT_AUTHENTICATED,
    USER_NOT_FOUND,
    ALREADY_IN_GAME,
    INVALID_MOVE,
    INVALID_PASSWORD,
    UNKNOWN_ACTION,
    INTERNAL_ERROR,
    MALFORMED_REQUEST,
    UNEXPECTED_MESSAGE_TYPE,
    UNEXPECTED_MESSAGE_ACTION;

    public static ErrorCodeType fromString(String string) {
        String normalized = string.replace("-", "_").toUpperCase();
        try { return ErrorCodeType.valueOf(normalized); }
        catch (IllegalArgumentException e) { return null; }
    }
}
