package iecd.a51597.server.protocol.types;

public enum ActionType {
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
        for (ActionType type : ActionType.values()) {
            if (type.toString().equalsIgnoreCase(string)) {
                return type;
            }
        }
        return null;
    }
}
