package work;

public class WorkersFailedToCompleteException extends Exception {
    public WorkersFailedToCompleteException(String cause) {
        super(cause);
    }
    public WorkersFailedToCompleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
