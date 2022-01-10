package matrixMultiplication;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import work.Worker;

public class FoxOttoMinPlus extends Worker {

    public FoxOttoMinPlus(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    // TODO: add method to Worker, which handles initial condition, e.g. setting c to +\infty

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
        if (l == 0) {
            store("C", Double.POSITIVE_INFINITY);
        }
        // In this iteration, we're computing A[i,k] + B[k, j]
        int k = (i + l) % n;

        // running minimum distance
        double curDist = read("C");
        double otherDist = read("a") + read("b");
        // we found a better path
        if (otherDist < curDist) {
            store("C", otherDist);
            if (k == j) {
                store("P", read("P"));
            } else {
                store("P", read("p"));
            }

            // TODO: we need a method to store and read integers, otherwise we can't do our predecessor ID thing
        }


        // TODO: also make method for what to do after all the computation. This would be nice, because then we could
        //       keep running totals as a field in the class (e.g. p and c), and then store in memory at the end

    }

    @Override
    protected void communicationBefore(int l) throws CommunicationChannelCongestionException {
        // TODO: have as initial compute
        if (l == 0) {
            store("b", read("B"));
            store("p", read("P"));
        }

        // one PE in each row uses the highway to broadcast it's A
        if (j == (i + l) % n) {
            broadcastRow(read("A"));
        }
        receiveRowBroadcast("a");

    }

    @Override
    protected void communicationAfter(int l) throws CommunicationChannelCongestionException {
        // we shift B and P upwards, wrapping around if necessary
        if (i == 0) {
            send(n - 1, j, read("b"));
            send(n - 1, j, read("P"));
        } else {
            send(i - 1, j, read("b"));
            send(i - 1, j, read("P"));
        }
        receive("b");
        receive("p");
    }
}
