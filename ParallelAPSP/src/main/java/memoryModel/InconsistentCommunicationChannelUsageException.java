package memoryModel;

public class InconsistentCommunicationChannelUsageException extends CommunicationChannelException {
    public InconsistentCommunicationChannelUsageException(String message, Throwable cause) {
        super(message, cause);
    }
    public InconsistentCommunicationChannelUsageException(String message) {
        super(message);
    }
}
