package iecd.a51597.common.protocol.types;

public enum ActionType {
    UNKNOWN,
    REGISTER,
    LOGIN,
    LOGOUT,
    UPDATE_PROFILE,
    SEARCH_USERS,
    GAME_INVITE,
    GAME_INVITE_RESPONSE,
    GAME_MOVE,
    GAME_OVER;

    public static ActionType fromString(String string) {
        String normalized = string.replace("-", "_").toUpperCase();
        try { return ActionType.valueOf(normalized); }
        catch (IllegalArgumentException e) { return null; }
    }
}
