package timingAnalysis.testWorkers;

import memoryModel.CommunicationChannelCongestionException;
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
 *   . . A
 *   D . B
 *   . . C
 *
 *  In the all phases, we do point to point sending A -> C -> B -> A.
 *  In all phases, D uses row broadcasting that B receives.
 *
 *  In the first phase, D will sleep for 4 seconds and C will sleep for 3 seconds.
 *  In the second phase, only C will sleep and for 2 seconds.
 */
public class TestWorker1 extends Worker {

    private String nodeID;

    public TestWorker1(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);

        if (i == 0 && j == 2) {
            nodeID = "A";
        } else if (i == 1 && j == 0) {
            nodeID = "D";
        } else if (i == 1 && j == 2) {
            nodeID = "B";
        } else if (i == 2 && j == 2) {
            nodeID = "C";
        } else {
            nodeID = ".";
        }
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) {
        try {
            if (nodeID.equals("D") && l == 0) {
                Thread.sleep(4000);
            } else if (nodeID.equals("C")) {
                Thread.sleep(3000 - 1000 * l);
            } else {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        // broadcasting
        if (nodeID.equals("D")) {
            broadcastRow(3.142716);
        }
        if (nodeID.equals("B")) {
            receiveRowBroadcast("row_broadcast");
        }

        // then point to point broadcasting
        switch (nodeID) {
            case "A":
                send(2, 2, 52);
                receive("data");
                break;
            case "B":
                send(0, 2, 3);
                receive("data");
                break;
            case "C":
                send(1, 2, 68);
                receive("data");
                break;
        }
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException { }
}
