package iecd.a51597.common.protocol.exceptions;

public class MessageParseException extends CommException {

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
