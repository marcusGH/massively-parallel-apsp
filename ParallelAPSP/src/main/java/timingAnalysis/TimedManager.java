package timingAnalysis;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TimedManager extends Manager {

    private final int p;
    private final Matrix<TimedWorker> timedWorkers;
    private final CountingMemoryController countingMemoryController;

    public TimedManager(Manager manager, Function<Integer, ? extends Topology> memoryTopology) throws WorkerInstantiationException {
        super(manager);

        this.p = manager.getProcessingElementGridSize();

        // decorate the memory controller and use it with dynamic dispatch
        Topology topology = memoryTopology.apply(this.p);
        this.countingMemoryController = new CountingMemoryController(manager.getMemoryController(), topology);
        this.setMemoryController(this.countingMemoryController);

        // decorate all the workers
        this.timedWorkers = new Matrix<>(this.p);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                // save a reference so we can extract times later
                TimedWorker tw = new TimedWorker(manager.getWorker(i, j));
                this.timedWorkers.set(i, j, tw);
                // use dynamic dispatch so that we don't need to reimplement doWork etc.
                this.setWorker(i, j, tw);
            }
        }
    }

    /**
     * Returns a list with one entry for each computation phase. Every entry is a matrix where entry (i, j) is the
     * time in nanoseconds processing element PE(i, j) took to complete its computation.
     * @return a list of matrix containing times
     */
    public List<Matrix<Long>> getComputationTimes() {
        List<Matrix<Long>> computationTimes = new ArrayList<>();
        // TODO: wrap doWork and count number of phases, so can do better limit
        for (int l = 0; l < this.timedWorkers.get(0, 0).getElapsedTimes().size(); l++) {
            Matrix<Long> elapsedTimes = new Matrix<>(this.p);
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    elapsedTimes.set(i, j, this.timedWorkers.get(i, j).getElapsedTimes().get(l));
                }
            }
            computationTimes.add(elapsedTimes);
        }
        return computationTimes;
    }

    /**
     * Note that every even-numbered item in the returned list will be statistics from the COMMUNICATION_BEFORE
     * phase and every odd-numbered item will be statistics from the COMMUNICATION_AFTER phase.
     * @return a list with 2*n elements, where n is the number of work phases
     */
    public List<Matrix<Integer>> getPointToPointCommunicationTimes() {
        assert this.countingMemoryController.getPointToPointCosts().size() % 2 == 0;
        return this.countingMemoryController.getPointToPointCosts();
    }

    public List<List<Integer>> getRowBroadcastCommunicationTimes() {
        assert this.countingMemoryController.getRowBroadcastCounts().size() % 2 == 0;
        return this.countingMemoryController.getRowBroadcastCounts();
    }

    public List<List<Integer>> getColBroadcastCommunicationTimes() {
        assert this.countingMemoryController.getColBroadcastCounts().size() % 2 == 0;
        return this.countingMemoryController.getColBroadcastCounts();
    }

    public static void main(String[] args) {
        // create the manager
        Manager manager;
        try {
            GraphReader graphReader = new GraphReader("../datasets/7-node-example.cedge");
            Matrix<Number> adjacencyMatrix = graphReader.getAdjacencyMatrix2(false);
            Matrix<Number> predMatrix = new Matrix<>(7, () -> 0);
            System.out.println(adjacencyMatrix);
            Map<String, Matrix<Number>> initialMemory = Map.of("A", adjacencyMatrix, "B", adjacencyMatrix, "P", predMatrix);
            manager = new Manager(7, 7, initialMemory, SquareGridTopology::new, FoxOtto.class);
        } catch (ParseException | WorkerInstantiationException e) {
            e.printStackTrace();
            return;
        }

        // decorate it
        TimedManager timedManager = null;
        try {
            timedManager = new TimedManager(manager, SquareGridTopology::new);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            return;
        }

        try {
            timedManager.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            return;
        }

        System.out.println(timedManager.getResult("dist"));

        for (int i = 0; i < timedManager.getComputationTimes().size(); i++) {
            System.out.println("Times for iteration " + i + ":");
            System.out.println(timedManager.getComputationTimes().get(i));
            System.out.println("---");
            System.out.println(timedManager.getPointToPointCommunicationTimes().get(i));
        }
    }
}
