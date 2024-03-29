package matrixMultiplication;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.CommunicationManager;
import memoryModel.PrivateMemory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * This class implements the min-plus matrix product computation, and can be used by passing the class to a
 * manager. It computes the min-plus product of a left matrix A and a right-matrix B. The resulting product
 * "dist" is independent of the provided predecessor matrix P. However, if A is equal to B and P is the predecessor
 * matrix for the graph A and B are distance matrices for, the result "pred" will be the predecessor matrix covering
 * paths of twice the length as P previously covered.
 *
 * The memory preconditions for this algorithm are:
 * <p>At memory location (i, j), there should be the following {@code PrivateMemory} content:
 *  <ul>
 *      <li>"A" maps to the element A[i, j] of the left matrix</li>
 *      <li>"B" maps to the element B[i, j] of the right matrix</li>
 *      <li>"P" maps to the element P[i, j] of the predecessor matrix</li>
 *  </ul>
 * </p>
 * <p>After work has been finished, the following results can be accessed with {@link work.Manager#getResult(String)}:
 * <ul>
 *     <li>"dist" the resulting min-plus matrix product</li>
 *     <li>"pred" the resulting predecessor matrix</li>
 * </ul>
 * </p>
 */
public class FoxOtto extends MinPlusProduct {

    public FoxOtto(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, CommunicationManager communicationManager) {
        super(i, j, p, n, numPhases, privateMemory, communicationManager);
    }

    /**
     * Before starting work, the Worker(i, j) assumes the following memory content:
     * "A" -> element A[i, j] of left matrix of product
     * "B" -> element B[i, j] of right matrix of product
     * "P" -> element P[i, j] of predecessor matrix
     */
    @Override
    public void initialisation() {
        // This is not the first management of this worker, so recall input (see main-loop in MatSquare)
        if (presentInMemory("dist")) {
            store("A", read("dist"));
            store("B", read("dist"));
            store("P", read("pred"));
        }
        // running total of least distance found so far
        store("dist", Double.POSITIVE_INFINITY); // represents C[i, j]
        // the "A" entry is never shifted, only broadcasted, so make a copy of it to prevent overwrite
        store("A_CONST", read("A"));
        // keep a default pred value in case we don't find any
        store("pred", read("P"));
    }

    /**
     * Worker(i, j) has received appropriate B
     *
     * @param l a non-negative integer representing number of computation phases already completed
     */
    @Override
    public void computation(int l) {
        // In this iteration, we're computing A[i,k] + B[k, j]
        int k = (i + l) % n;

        // running minimum distance
        double curDist = readDouble("dist");
        double otherDist = readDouble("A") + readDouble("B");
        // we found a better path
        if (otherDist < curDist) {
            store("dist", otherDist);
            // only update predecessor if it doesn't cause loops
            if (k == j) {
                store("pred", readInt("pred"));
            } else {
                store("pred", readInt("P"));
            }
        }
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        // one PE in each row uses the highway to broadcast it's A
        if (j == (i + l) % n) {
            broadcastRow(readDouble("A_CONST"));
        }
        receiveRowBroadcast("A");

    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        // we shift B and P upwards, wrapping around if necessary
        if (i == 0) {
            send(n - 1, j, readDouble("B"));
            send(n - 1, j, readDouble("P"));
        } else {
            send(i - 1, j, readDouble("B"));
            send(i - 1, j, readDouble("P"));
        }
        receive("B");
        receive("P");
    }

    public static void main(String[] args) {
        int num_iters = 1000;

        PrivateMemory pm = new PrivateMemory(1);
        pm.set("A", 100000);
        pm.set("B", 3.14);
        pm.set("P", 5);
        FoxOtto worker = new FoxOtto(4, 3, 6, 6, 6, pm, null);
        worker.initialisation();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long timeBefore = threadMXBean.getCurrentThreadCpuTime();
        for (int i = 0; i < num_iters; i++) {
            worker.computation(i);
            pm.set("A", Math.random() * 100000);
        }
        long timeElapsed = threadMXBean.getCurrentThreadCpuTime() - timeBefore;
        System.out.println("Compute time: " + timeElapsed);
        timeBefore = threadMXBean.getCurrentThreadCpuTime();
        for (int i = 0; i < num_iters; i++) {
            int a = (int) (Math.random() * 1000);
        }
        long randTime = threadMXBean.getCurrentThreadCpuTime() - timeBefore;
        System.out.println("Rand time: " + randTime);
        System.out.println((double) (timeElapsed - randTime) / num_iters);
    }
}
