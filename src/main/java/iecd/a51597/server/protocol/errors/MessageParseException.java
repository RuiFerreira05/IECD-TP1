package iecd.a51597.server.protocol.errors;

public class MessageParseException extends CommError {

    public MessageParseException() {
        super();
    }

    public MessageParseException(String message) {
        super(message);
    }

    public MessageParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
