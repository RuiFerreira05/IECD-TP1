package iecd.a51597.common.protocol.types;

public enum MessageType {
    REQUEST,
    RESPONSE,
    PUSH;

    public static MessageType fromString(String string) {
        String normalized = string.replace("-", "_").toUpperCase();
        try { return MessageType.valueOf(normalized); }
        catch (IllegalArgumentException e) { return null; }
    }
}
