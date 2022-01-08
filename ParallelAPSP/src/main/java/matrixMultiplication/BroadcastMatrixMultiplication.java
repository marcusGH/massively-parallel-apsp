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
    protected void computation(int l) {
        if (l == 0) {
            store("C", Double.POSITIVE_INFINITY);
        }
        double value = Math.min(read("rowA") + read("colB"), read("C"));
        store("C", value);
    }

    @Override
    protected void communicationBefore(int l) throws CommunicationChannelCongestionException {
        if (i == l) {
            broadcastCol(read("B"));
        }
        if (j == l) {
            broadcastRow(read("A"));
        }
        receiveColBroadcast("colB");
        receiveRowBroadcast("rowA");
    }

    @Override
    protected void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}
