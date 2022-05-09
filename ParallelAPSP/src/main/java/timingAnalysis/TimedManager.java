package timingAnalysis;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import memoryModel.CommunicationChannelException;
import timingAnalysis.topology.SquareGridTopology;
import timingAnalysis.topology.Topology;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.Map;
import java.util.function.Function;

public class TimedManager extends Manager {

    private final int p;
    private final int problemSize;
    private final Matrix<TimedWorker> timedWorkers;
    private final TimedCommunicationManager timedCommunicationManager;

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
        this.problemSize = manager.getProblemSize();

        // We decorate the communication manager with timing analyses functionality
        Topology topology = memoryTopology.apply(this.p);
        this.timedWorkers = new Matrix<>(this.p);
        this.timedCommunicationManager = new TimedCommunicationManager(manager.getCommunicationManager(),
                this.timedWorkers, topology, multiprocessorAttributes);
        // then use it instead of the existing one with dynamic dispatch
        this.setCommunicationManager(this.timedCommunicationManager);

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

    public TimingAnalysisResult getTimingAnalysisResult() {
        return new TimingAnalysisResult(this.timedCommunicationManager, this.problemSize);
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
        TimingAnalysisResult result = timedManager.getTimingAnalysisResult();
        System.out.println(result.getComputationTimes());
        System.out.println("---");
        System.out.println(result.getSendTimes());
        System.out.println("---");
        System.out.println(result.getTotalCommunicationTimes());

    }
}
