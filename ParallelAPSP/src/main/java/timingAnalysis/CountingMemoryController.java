package timingAnalysis;

import javafx.util.Pair;
import memoryModel.CommunicationChannelCongestionException;
import memoryModel.InconsistentCommunicationChannelUsageException;
import memoryModel.MemoryController;
import memoryModel.topology.Topology;
import util.Matrix;

import java.util.*;

/**
 * TODO: rename the class
 *
 * Simulates the communication dependencies in a MIMD multiprocessor.  When wrapping this class around a
 * memoryController, it will keep track of the simulates real-time state of each of the passed timedWorkers.
 *
 * The state of the processing elements is represented by a matrix of times, {@code currentWorkerTimes}. Entry (i, j)
 * in the matrix represents the number of nanoseconds PE(i, j) has been running for since the start of computation,
 * counting both computation and communication time. This state is updated every time {@link MemoryController#flush()}
 * is called. The update happens as follows:
 * <ul>
 *     <li>The measured computation time for PE(i, j) is added to the currentWorkerTimes</li>
 *     <li>The estimated time to communicate (point to point and broadcasting) data from PE(i, j) is added
 *         to the currentWorkerTimes state</li>
 *     <li>The estimated stall time for PE(i, j) is calculated: Let S be the set of all processing elements
 *         sending data to PE(i, j). Stall time = max(0, max_{w' in S} currentWorkerTimes[w']). In other words,
 *         it must wait until all the data send to it is ready. This stall time is then added to the state
 *         currentWorkerTimes.</li>
 * </ul>
 *
 * TODO: rename this class
 *
 */
public class CountingMemoryController extends MemoryController {

    // processing element grid size
    private final int p;

    private final Topology memoryTopology;
    private final MultiprocessorAttributes multiprocessorAttributes;

    // keep track of current times of all the workers
    private Matrix<Double> currentWorkerTimes;
    private final Matrix<Double> cumulativeWorkerCommunicationTimes;
    private final Matrix<Double> cumulativeWorkerStallTimes;
    private final Matrix<Double> cumulativeWorkerComputationTimes;

    private final Matrix<Integer> workerBytesSent;
    private final Matrix<Integer> workerRowBroadcastBytesSent;
    private final Matrix<Integer> workerColBroadcastBytesSent;

    private final Matrix<Integer> sendingDistance;

    private final Matrix<TimedWorker> timedWorkers;

    private final int communicationPhasesCompleted;


    /**
     *
     * @param memoryController this memory controller should be the same one used by the timedWorkers.
     *                         Otherwise, behaviour is undefined.
     * @param timedWorkers
     * @param memoryTopology
     * @param multiprocessorAttributes
     */
    public CountingMemoryController(MemoryController memoryController, Matrix<TimedWorker> timedWorkers,
                                    Topology memoryTopology, MultiprocessorAttributes multiprocessorAttributes) {
        super(memoryController);
        this.p = memoryController.getProcessingElementGridSize();
        this.timedWorkers = timedWorkers;
        this.memoryTopology = memoryTopology;
        this.multiprocessorAttributes = multiprocessorAttributes;

        // timing trackers (these are cumulative so never reset)
        this.currentWorkerTimes = new Matrix<>(this.p, () -> 0.0);
        this.cumulativeWorkerCommunicationTimes = new Matrix<>(this.p, () -> 0.0);
        this.cumulativeWorkerStallTimes = new Matrix<>(this.p, () -> 0.0);
        this.cumulativeWorkerComputationTimes = new Matrix<>(this.p, () -> 0.0);

        // num bytes sent trackers (reset after each communication phase)
        this.workerBytesSent = new Matrix<>(this.p, () -> 0);
        this.workerRowBroadcastBytesSent = new Matrix<>(this.p, () -> 0);
        this.workerColBroadcastBytesSent = new Matrix<>(this.p, () -> 0);

        // keep track of sending distance, based on topology
        this.sendingDistance = new Matrix<>(this.p);

        // for convenience
        this.communicationPhasesCompleted = 0;
    }

    private synchronized double getStallTime(int sendI, int sendJ, int receiveI, int receiveJ) {
        // the receiver must wait until the sender has completed computation, so receiver
        //   is the one that is stalling, but just if the sender takes longer
        return Math.max(0.0, this.currentWorkerTimes.get(sendI, sendJ)
                - this.currentWorkerTimes.get(receiveI, receiveJ));
    }

    private int inferObjectSize(Number n) {
        if (n instanceof  Double) {
            return Double.BYTES;
        } else if (n instanceof Integer) {
            return Integer.BYTES;
        } else if (n instanceof Long) {
            return Long.BYTES;
        } else if (n instanceof Short) {
            return Short.BYTES;
        } else {
            // assume 64 bit if don't know
            return 8;
        }
    }

    /**
     * We assume all the memory transfers by different PE can happen in parallell, so this method counts the number of
     * transfers originating from each node, and the "cost" associated with each of these. When later computating the
     * communication cost, we would then take the maximum of the "sum of send-costs".
     *
     * For functionality, see {@link MemoryController#sendData(int, int, int, int, Number)}
     */
    @Override
    public void sendData(int sendI, int sendJ, int receiveI, int receiveJ, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.workerBytesSent) {
            // count the number of bytes sent
            this.workerBytesSent.set(sendI, sendJ, this.workerBytesSent.get(sendI, sendJ) + inferObjectSize(value));
            // We assume each Worker[i, j] only sends data to some unique Worker[i', j'] each phase
            this.sendingDistance.set(sendI, sendJ, this.memoryTopology.distance(sendI, sendJ, receiveI, receiveJ));
        }
        // then execute the same functionality
        super.sendData(sendI, sendJ, receiveI, receiveJ, value);
    }

    @Override
    public void broadcastRow(int i, int j, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.workerRowBroadcastBytesSent) {
            // counter the number of bytes sent
            this.workerRowBroadcastBytesSent.set(i, j, this.workerRowBroadcastBytesSent.get(i, j) + inferObjectSize(value));
        }
        // and execute same functionality
        super.broadcastRow(i, j, value);
    }

    @Override
    public void broadcastCol(int i, int j, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.workerColBroadcastBytesSent) {
            // counter the number of bytes sent
            this.workerColBroadcastBytesSent.set(i, j, this.workerColBroadcastBytesSent.get(i, j) + inferObjectSize(value));
        }
        // and execute same functionality
        super.broadcastCol(i, j, value);
    }

    @Override
    public synchronized void flush() throws InconsistentCommunicationChannelUsageException {
        // We first add all the communication time associated with sending/broadcasting data (not counting stalls)
        //   as well as time spent on any computation phases between this flush and the previous one
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                // computation time in nanoseconds
                double computationTime = this.timedWorkers.get(i, j).getElapsedTime();
                // if we have two communication phases right after each other, we should assume zero computation
                //   time between them
                this.timedWorkers.get(i, j).resetElapsedTime();

                // point to point send time = number_of_hops * time_per_hop
                double sendTime = this.multiprocessorAttributes.getSendTime(
                        this.workerBytesSent.get(i, j), false
                ) * this.sendingDistance.get(i, j);

                // add broadcast send time (row)
                sendTime += this.multiprocessorAttributes.getSendTime(
                        this.workerRowBroadcastBytesSent.get(i, j), true
                );

                // add broadcast send time (column)
                sendTime += this.multiprocessorAttributes.getSendTime(
                        this.workerColBroadcastBytesSent.get(i, j), true
                );

                // update current time
                this.currentWorkerTimes.set(i, j, this.currentWorkerTimes.get(i, j) + computationTime + sendTime);
                this.cumulativeWorkerCommunicationTimes.set(i, j,
                        this.cumulativeWorkerCommunicationTimes.get(i, j) + sendTime);
                this.cumulativeWorkerComputationTimes.set(i, j,
                        this.cumulativeWorkerComputationTimes.get(i, j) + computationTime);
            }
        }

        // make a copy because changing currentWorkerTimes during update will cause getStallTimes to give incorrect
        //   values at it inspects values in currentWorkerTimes when computing stall times
        Matrix<Double> newCurrentWorkerTimes = new Matrix<>(this.currentWorkerTimes);

        // We now account for the communication time causes by stalls: If worker W1 receives data from W2 and
        //   W1 finishes computation first, it needs to stall until W2 has sent the data.
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                // Post-condition of iteration(i, j):
                //   currentWorkerTimes[i, j] is the time of Worker[i, j] after preceding computation
                //   phase and the communication phase that is being flushed, accounting for latency, stalls, etc.

                // first find the time we need to stall, looking at Worker[i, j] as a receiver
                double stallTime = 0.0;

                List<Optional<Pair<Integer, Integer>>> senderIDs = Arrays.asList(
                        senderToRecipientID.get(i, j), rowBroadcasterID.get(i), colBroadcasterID.get(j));
                for (Optional<Pair<Integer, Integer>> id : senderIDs) {
                    // we look at all the possible senders that are sending to Worker[i, j] and then find the one
                    //   sending at the latest possible time, and find the stall wait caused by that
                    if (id.isPresent()) {
                        // We look at Worker[i, j] as the **receiver**, because the receiver is the one that must stall
                        stallTime = Math.max(stallTime, getStallTime(id.get().getKey(), id.get().getValue(), i, j));
                    }
                }
                // count time needed to wait for value to be ready as "communication time"
                this.cumulativeWorkerStallTimes.set(i, j,
                        this.cumulativeWorkerStallTimes.get(i, j) + stallTime);
                newCurrentWorkerTimes.set(i, j, newCurrentWorkerTimes.get(i, j) + stallTime);
            }
        }

        // update the current times
        this.currentWorkerTimes = newCurrentWorkerTimes;

        // reset the bytes trackers
        this.workerBytesSent.setAll(() -> 0);
        this.workerRowBroadcastBytesSent.setAll(() -> 0);
        this.workerColBroadcastBytesSent.setAll(() -> 0);

        // the actual functionality must be performed last because it resets the sender IDs, that we use above
        super.flush();
    }

    public int getCommunicationPhasesCompleted() {
        return communicationPhasesCompleted;
    }
}
