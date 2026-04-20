package iecd.a51597.common.protocol.exceptions;

public class MalformedMessageException extends CommException {

    public MalformedMessageException() {
        super();
    }

    public MalformedMessageException(String message) {
        super(message);
    }

    public MalformedMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
