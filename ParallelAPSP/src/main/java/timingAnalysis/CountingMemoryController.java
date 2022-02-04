package timingAnalysis;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.InconsistentCommunicationChannelUsageException;
import memoryModel.MemoryController;
import memoryModel.topology.Topology;
import util.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CountingMemoryController extends MemoryController {

    // processing element grid size
    private final int p;

    // to limit the amount of memory uses to track communication
    private int disableAfterNPhases;
    private int communicationPhasesCompleted;

    private final Topology memoryTopology;
    // we have one entry in the lists for each communication phase
    private final List<Matrix<Integer>> allCommunicationChannelCosts;
    private final List<List<Integer>> allRowBroadcastCounts;
    private final List<List<Integer>> allColBroadcastCounts;
    private final Matrix<Integer> runningCommunicationChannelCost;
    private final List<Integer> runningRowBroadcastCounts;
    private final List<Integer> runningColBroadcastCounts;

    public CountingMemoryController(MemoryController memoryController, Topology memoryTopology) {
        super(memoryController);
        this.p = memoryController.getProcessingElementGridSize();
        this.memoryTopology = memoryTopology;

        // create the total counts
        this.allCommunicationChannelCosts = new ArrayList<>();
        this.allRowBroadcastCounts = new ArrayList<>();
        this.allColBroadcastCounts = new ArrayList<>();

        // and initialize running counters
        this.runningCommunicationChannelCost = new Matrix<>(p, () -> 0);
        this.runningRowBroadcastCounts = Stream.generate(() -> 0).limit(p).collect(Collectors.toList());
        this.runningColBroadcastCounts = Stream.generate(() -> 0).limit(p).collect(Collectors.toList());

        this.communicationPhasesCompleted = 0;
        this.disableAfterNPhases = -1;
    }

    public void disableAfterNPhases(int n) {
        this.disableAfterNPhases = n;
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
        synchronized (this.runningCommunicationChannelCost) {
            // record the communication cost of point-to-point, and accumulate on the counter
            int newCost = this.runningCommunicationChannelCost.get(sendI, sendJ)
                    + this.memoryTopology.distance(sendI, sendJ, receiveI, receiveJ);
            this.runningCommunicationChannelCost.set(sendI, sendJ, newCost);
        }
        // then execute the same functionality
        super.sendData(sendI, sendJ, receiveI, receiveJ, value);
    }

    @Override
    public void broadcastRow(int i, int j, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.runningRowBroadcastCounts) {
            // increment
            this.runningRowBroadcastCounts.set(i, this.runningRowBroadcastCounts.get(i) + 1);
        }
        // and execute same functionality
        super.broadcastRow(i, j, value);
    }

    @Override
    public void broadcastCol(int i, int j, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.runningColBroadcastCounts) {
            // increment
            this.runningColBroadcastCounts.set(j, this.runningColBroadcastCounts.get(j) + 1);
        }
        // and execute same functionality
        super.broadcastCol(i, j, value);
    }

    @Override
    public synchronized void flush() throws InconsistentCommunicationChannelUsageException {
        // stop saving data after this point
        if (this.disableAfterNPhases == -1 || this.communicationPhasesCompleted < this.disableAfterNPhases) {
            // save the communication costs counted so far (make a deep copy so that it's not 0'ed)
            this.allCommunicationChannelCosts.add(new Matrix<>(this.runningCommunicationChannelCost));
            this.allRowBroadcastCounts.add(new ArrayList<>(this.runningRowBroadcastCounts));
            this.allColBroadcastCounts.add(new ArrayList<>(this.runningColBroadcastCounts));
        }
        // and reset the counts for the next communication phase
        this.runningCommunicationChannelCost.setAll(() -> 0);
        for (int i = 0; i < this.p; i++) {
            this.runningColBroadcastCounts.set(i, 0);
            this.runningRowBroadcastCounts.set(i, 0);
        }
        // increment
        this.communicationPhasesCompleted++;
        // then perform the standard functionality
        super.flush();
    }

    public List<Matrix<Integer>> getPointToPointCosts() {
        return this.allCommunicationChannelCosts;
    }

    public List<List<Integer>> getRowBroadcastCounts() {
        return this.allRowBroadcastCounts;
    }

    public List<List<Integer>> getColBroadcastCounts() {
        return this.allColBroadcastCounts;
    }
}
