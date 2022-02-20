package timingAnalysis.testWorkers;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.CommunicationChannelException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import work.Worker;

/**
 * To be able to pass a Worker class to the WorkerFactory in our tests, it's necessary for the Worker subclass
 * to be defined in the "work" package, or for the class itself to be public.
 * Therefore, this package is just a list of classes we use for testing.
 */

/**
 * We have 3 x 3 workers:
 *   A B C
 *   D . .
 *   . . E
 *
 *  In the all phases, we do point to point sending A -> D, B -> C, C -> A
 *  In all phases, D uses row broadcasting and E does column broadcasting.
 *
 *  The number of sends is [1, 3, 6]
 */
public class TestWorker2 extends Worker {

    private String nodeID;
    private int[] numSends = {1, 3, 6};

    public TestWorker2(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);

        if (i == 0 && j == 0) {
            nodeID = "A";
        } else if (i == 0 && j == 1) {
            nodeID = "B";
        } else if (i == 0 && j == 2) {
            nodeID = "C";
        } else if (i == 1 && j == 0) {
            nodeID = "D";
        } else if (i == 2 && j == 2) {
            nodeID = "E";
        } else {
            nodeID = ".";
        }
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doSending(int l, int receiveI, int receiveJ) throws CommunicationChannelCongestionException {
        for (int k = 0; k < this.numSends[l]; k++) {
            send(receiveI, receiveJ, 42);
        }
    }
    private void doReciving(int l) {
        for (int k = 0; k < this.numSends[l]; k++) {
            receive("data");
        }
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        // then point to point broadcasting
        switch (nodeID) {
            case "A":
                doSending(l, 1, 0);
                doReciving(l);
                break;
            case "B":
                doSending(l, 0, 2);
                break;
            case "C":
                doSending(l, 0, 0);
                doReciving(l);
                break;
            case "D":
                doReciving(l);
                break;
        }
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        if (nodeID.equals("D")) {
            broadcastRow(3.14);
        }
        if (nodeID.equals("E")) {
            broadcastCol(42.1);
        }
    }
}
