package memoryModel;

public class WorkersFailedToCompleteException extends Exception {
    public WorkersFailedToCompleteException(String cause) {
        super(cause);
    }
}
