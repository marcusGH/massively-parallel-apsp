package matrixMultiplication;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import work.Worker;

/**
 * This worker assumes two input matrices, stored in location "A" and "B", respectively.
 * The worker will then together multiply the matrices and store the result in "C".
 */
public class BroadcastMatrixMultiplication extends Worker {

    public BroadcastMatrixMultiplication(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    public void initialise() {

    }

    @Override
    public void computation(int l) {
        if (l == 0) {
            store("C", 0);
        }
        double value = readDouble("rowA") * readDouble("colB") + readDouble("C");
        store("C", value);
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        if (i == l) {
            broadcastCol(readDouble("B"));
        }
        if (j == l) {
            broadcastRow(readDouble("A"));
        }
        receiveColBroadcast("colB");
        receiveRowBroadcast("rowA");
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}
