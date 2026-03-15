package iecd.a51597.server.protocol.errors;

public abstract class CommError extends Exception {

    public CommError() {
        super();
    }

    public CommError(String message) {
        super(message);
    }

    public CommError(String message, Throwable cause) {
        super(message, cause);
    }
}
