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
        int witness = -1; // ????

        // running minimum distance
        double curDist = read("C");
        double otherDist = read("a") + read("b");
        // we found a better path
        if (otherDist < curDist) {
            store("C", otherDist);


            // TODO: we need a method to store and read integers, otherwise we can't do our predecessor ID thing
        }



    }

    @Override
    protected void communicationBefore(int l) throws CommunicationChannelCongestionException {

    }

    @Override
    protected void communicationAfter(int l) throws CommunicationChannelCongestionException {

    }
}
