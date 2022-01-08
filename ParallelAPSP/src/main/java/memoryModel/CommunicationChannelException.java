package memoryModel;

public class CommunicationChannelException extends Exception {
    public CommunicationChannelException() { }

    public CommunicationChannelException(String message) {
        super(message);
    }

    public CommunicationChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
