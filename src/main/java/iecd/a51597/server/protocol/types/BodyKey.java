package iecd.a51597.server.protocol.types;

public enum BodyKey {
    USERNAME,
    PASSWORD,
    PHOTO,
    STATUS,
    ERROR,
    SESSION,
    USER,
    QUERY,
    RESULTS,
    TARGET_USER_ID,
    GAME_ID,
    FROM_USER_ID,
    FROM_USERNAME,
    ACCEPT,
    ACCEPTED,
    OPPONENT_USERNAME,
    MOVE,
    WINNER_ID,
    WINNER_USERNAME;

    public static BodyKey fromString(String string) {
        String normalized = string.replace("-", "_").toUpperCase();
        try { return BodyKey.valueOf(normalized); }
        catch (IllegalArgumentException e) { return null; }
    }
}
