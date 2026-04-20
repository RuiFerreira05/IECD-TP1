package iecd.a51597.common.protocol.exceptions;

public abstract class CommException extends Exception {

    public CommException() {
        super();
    }

    public CommException(String message) {
        super(message);
    }

    public CommException(String message, Throwable cause) {
        super(message, cause);
    }
}
