package matrixMultiplication;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import work.Worker;

public class FoxOttoMinPlus extends Worker {

    public FoxOttoMinPlus(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    /**
     * Before starting work, the Worker(i, j) assumes the following memory content:
     * "A" -> element A[i, j] of left matrix of product
     * "B" -> element B[i, j] of right matrix of product
     * "P" -> element P[i, j] of predecessor matrix
     */
    @Override
    protected void initialise() {
        // running total of least distance found so far
        store("dist", Double.POSITIVE_INFINITY); // represents C[i, j]
        // the "A" entry is never shifted, only broadcasted, so make a copy of it to prevent overwrite
        store("A_CONST", read("A"));
        // keep a default pred value in case we don't find any
        store("pred", read("P")); // TODO: this.j or "P"? Does it make a difference?
    }

    /**
     * Before computation, the worker (i, j) assumes the following content in memory at iteration l:
     * "a" -> element from left  matrix of product A x B, specifically A[i, ?]
     * "b" -> element from right matrix of product A x B, specifically B[?, j]
     * "p" -> element from predecessor matrix, specifically P[?, ?]
     *
     * The worker will then store the partial matrix product result for C[i,j] with label "C", and
     * also store the running predecessor element P[i,j] with label "P"
     *
     * @param l a non-negative integer representing number of computation phases already completed
     */
    @Override
    protected void computation(int l) {
        // In this iteration, we're computing A[i,k] + B[k, j]
        int k = (i + l) % n;

        // running minimum distance
        double curDist = readDouble("dist");
        double otherDist = readDouble("A") + readDouble("B");
        // we found a better path
        if (otherDist < curDist) {
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
    protected void communicationBefore(int l) throws CommunicationChannelCongestionException {
        // one PE in each row uses the highway to broadcast it's A
        if (j == (i + l) % n) {
            broadcastRow(readDouble("A_CONST"));
        }
        receiveRowBroadcast("A");

    }

    @Override
    protected void communicationAfter(int l) throws CommunicationChannelCongestionException {
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
