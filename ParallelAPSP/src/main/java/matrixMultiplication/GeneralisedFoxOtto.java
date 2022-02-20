package matrixMultiplication;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
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
public class GeneralisedFoxOtto extends MinPlusProduct {

    private final int subMatrixSize;

    public GeneralisedFoxOtto(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
        this.subMatrixSize = n / p;
    }

    /**
     * Before starting work, the Worker(i, j) assumes the following memory content:
     * "A" -> element A[i, j] of left matrix of product
     * "B" -> element B[i, j] of right matrix of product
     * "P" -> element P[i, j] of predecessor matrix
     */
    @Override
    public void initialise() {
        for (int i2 = 0; i2 < subMatrixSize; i2++) {
            for (int j2 = 0; j2 < subMatrixSize; j2++) {
                // This is not the first management phase, so reinitialize input with result from
                //   previous management iteration
                if (presentInMemory(i2, j2, "dist")) {
                    store(i2, j2, "A", read(i2, j2, "dist"));
                    store(i2, j2, "B", read(i2, j2, "dist"));
                    store(i2, j2, "P", read(i2, j2, "pred"));
                }

                // running total of least distance found so far
                store(i2, j2, "dist", Double.POSITIVE_INFINITY); // represents C[i, j]
                // the "A" entry is never shifted, only broadcasted, so make a copy of it to prevent overwrite
                store(i2, j2, "A_CONST", read(i2, j2,"A"));
                // keep a default pred value in case we don't find any
                store(i2, j2, "pred", read(i2, j2, "P")); // TODO: this.j or "P"? Does it make a difference?

                // we are using integer weights instead
                if (read(i2, j2, "A") instanceof Integer) {
                    store(i2, j2, "dist", Integer.MAX_VALUE);
                }
            }
        }
    }

    /**
     * Worker(i, j) has received appropriate B
     *
     * @param l a non-negative integer representing number of computation phases already completed
     */
    @Override
    public void computation(int l) {
        // handle the whole sub-matrix
        for (int i2 = 0; i2 < subMatrixSize; i2++) {
            for (int j2 = 0; j2 < subMatrixSize; j2++) {

                // We are partially computing C[size * i + i2, size * j + j2] at corresponding PE, so let
                //   i' = size * i + i2, j' = size * j + j2
                for (int m = 0; m < subMatrixSize; m++) {
                    // When computing C[i', j'] at l=0, which should start with k such that
                    //   k = i'. This is to be consistent with the non-generalized version and
                    //   is necessary to get the predecessor pointers right
                    int iter = (i2 + m) % subMatrixSize;
                    // In this iteration, we are computing A[i', k] + B[k, j'], where
                    int k = (subMatrixSize * (i + l) + iter) % n;

                    double curDist = readDouble(i2, j2, "dist");
                    double otherDist = readDouble(i2, iter, "A") + readDouble(iter, j2, "B");

                    // found better distance
                    if (otherDist < curDist) {
                        store(i2, j2, "dist", otherDist);
                        // only update predecessor if it does not cause loops i.e. if k != j'
                        if (k != subMatrixSize * j + j2) {
                            store(i2, j2, "pred", readInt(iter, j2, "P"));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        // operate on the whole sub-matrix
        for (int i2 = 0; i2 < subMatrixSize; i2++) {
            for (int j2 = 0; j2 < subMatrixSize; j2++) {
                // one PE in each row uses the highway to broadcast its sub-matrix A,
                //   starting with the diagonal and then shifting it right
                if (j == (i + l) % p) {
                    broadcastRow(readDouble(i2, j2, "A_CONST"));
                }
                receiveRowBroadcast(i2, j2, "A");
            }
        }
    }


    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        // we shift B and P upwards, wrapping around if necessary
        for (int i2 = 0; i2 < subMatrixSize; i2++) {
            for (int j2 = 0; j2 < subMatrixSize; j2++) {
                // we must wrap around if reach the end when sending data North
                int sendLoc = i == 0 ? (p - 1) : i - 1;

                send(sendLoc, j, readDouble(i2, j2, "B"));
                send(sendLoc, j, readDouble(i2, j2, "P"));
                receive(i2, j2, "B");
                receive(i2, j2, "P");
            }
        }
    }
}
