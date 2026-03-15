package iecd.a51597.server.protocol.types;

public enum MessageType {
    REQUEST,
    RESPONSE,
    PUSH;

    public static MessageType fromString(String string) {
        for (MessageType type : MessageType.values()) {
            if (type.toString().equalsIgnoreCase(string)) {
                return type;
            }
        }
        return null;
    }
}
