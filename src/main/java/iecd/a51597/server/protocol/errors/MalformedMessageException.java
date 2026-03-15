package iecd.a51597.server.protocol.errors;

public class MalformedMessageException extends CommError {

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
