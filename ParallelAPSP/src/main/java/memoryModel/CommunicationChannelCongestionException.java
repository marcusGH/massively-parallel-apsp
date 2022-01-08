package memoryModel;

public class CommunicationChannelCongestionException extends CommunicationChannelException {
    public CommunicationChannelCongestionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationChannelCongestionException(String message) {
        super(message);
    }
}
