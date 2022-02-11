package timingAnalysis;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import matrixMultiplication.GeneralisedFoxOtto;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.Map;
import java.util.function.Function;

public class TimedManager extends Manager {

    private final int p;
    private final Matrix<TimedWorker> timedWorkers;
    private final CountingMemoryController countingMemoryController;

    /**
     * Decorates a passed Manager object with functionality to measure the computation time and estimate the
     * communication time of the workers managed by the passer manager. These execution times can be returned
     * after the manager has completed its work.
     *
     * @param manager the manager to decorate
     * @param multiprocessorAttributes a specification of the communication and computation hardware used
     * @param memoryTopology a constructor taking a non-negative integer and giving an object that subtypes Topology
     * @throws WorkerInstantiationException if the timed workers fail to instantiate
     */
    public TimedManager(Manager manager, MultiprocessorAttributes multiprocessorAttributes,
                        Function<Integer, ? extends Topology> memoryTopology) throws WorkerInstantiationException {
        super(manager);

        this.p = manager.getProcessingElementGridSize();

        // We decorate the memory controller with timing analyses functionality
        Topology topology = memoryTopology.apply(this.p);
        this.timedWorkers = new Matrix<>(this.p);
        this.countingMemoryController = new CountingMemoryController(manager.getMemoryController(),
                this.timedWorkers, topology, multiprocessorAttributes);
        // then use it instead of the existing one with dynamic dispatch
        this.setMemoryController(this.countingMemoryController);

        // decorate all the workers with timing behaviour
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

    public void enableFoxOttoTimeAveraging(int num_iters) {
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                this.timedWorkers.get(i, j).enableAverageComputeTimes(num_iters);
            }
        }
    }

    public Matrix<Double> getComputationTimes() {
        return this.countingMemoryController.getWorkerComputationTimes();
    }

    public Matrix<Double> getTotalCommunicationTimes() {
        Matrix<Double> communicationTimes = new Matrix<>(this.countingMemoryController.getWorkerCommunicationTimes());
        // add time the workers stall
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                communicationTimes.set(i, j, communicationTimes.get(i, j) +
                        this.countingMemoryController.getWorkerStallTimes().get(i, j));
            }
        }
        return communicationTimes;
    }

    public Matrix<Double> getStallTimes() {
        return this.countingMemoryController.getWorkerStallTimes();
    }

    public Matrix<Double> getSendTimes() {
        return this.countingMemoryController.getWorkerCommunicationTimes();
    }

    public Matrix<Double> getTotalExecutionTimes() {
        return this.countingMemoryController.getTotalWorkerTimes();
    }

    public static void main(String[] args) {
        // create the manager
        Manager manager;
        try {
            GraphReader graphReader = new GraphReader("../test-datasets/SF-d-40.cedge", true);
            int n = graphReader.getNumberOfNodes();
            Matrix<Number> adjacencyMatrix = graphReader.getAdjacencyMatrix();
            Matrix<Number> predMatrix = new Matrix<>(n, () -> 0);
//            System.out.println(adjacencyMatrix);
            Map<String, Matrix<Number>> initialMemory = Map.of("A", adjacencyMatrix, "B", adjacencyMatrix, "P", predMatrix);
            manager = new Manager(n, n, n, initialMemory, FoxOtto.class);
        } catch (ParseException | WorkerInstantiationException e) {
            e.printStackTrace();
            return;
        }

        // decorate it
        TimedManager timedManager;
        try {
            timedManager = new TimedManager(manager, new MultiprocessorAttributes(), SquareGridTopology::new);
            timedManager.enableFoxOttoTimeAveraging(100);
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
        System.out.println(timedManager.getComputationTimes());
        System.out.println("---");
        System.out.println(timedManager.getSendTimes());
        System.out.println("---");
        System.out.println(timedManager.getTotalCommunicationTimes());

    }
}
