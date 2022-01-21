package matrixMultiplication;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;

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

    public FoxOtto(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    /**
     * Before starting work, the Worker(i, j) assumes the following memory content:
     * "A" -> element A[i, j] of left matrix of product
     * "B" -> element B[i, j] of right matrix of product
     * "P" -> element P[i, j] of predecessor matrix
     */
    @Override
    public void initialise() {
        // running total of least distance found so far
        store("dist", Double.POSITIVE_INFINITY); // represents C[i, j]
        // the "A" entry is never shifted, only broadcasted, so make a copy of it to prevent overwrite
        store("A_CONST", read("A"));
        // keep a default pred value in case we don't find any
        store("pred", read("P")); // TODO: this.j or "P"? Does it make a difference?
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
            if (this.i == 0 && this.j == 4) {
                System.out.println("Old dist is " + curDist + " and new is " + otherDist);
            }
            store("dist", otherDist);
            // only update predecessor if it doesn't cause loops
            if (k == j) { // or is it readInt("P")???
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
}
