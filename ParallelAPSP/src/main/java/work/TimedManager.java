package work;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import timingAnalysis.CountingMemoryController;
import util.Matrix;

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

        // TODO: Awright, in the Worker class, we are using lambdas to return Callables, so all the variables there,
        //       inclduing the memoryController **must be effictively final** because Java compiler makes a copy of them,
        //       so if a setter is used, this new reference won't actually be used in the computation and communication :O
        //       As a result, the only way to get around this is to recreate the workers with the adapted MemoryController
        //       using a WorkerFactory here in the TimedManager. Another problem was the Manager not having the same reference,
        //       but this is fixed by calling this.Set... and not manager.set...
        // TODO: in order:
        //       * Think about whether to different package this class,then use workerFactory properly with e.g. getters
        //       * Clean up the code here more proper
        //       * Refactor packages as suitable, then cleanup the code in affected files a bit ...

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

    public List<Matrix<Integer>> getPointToPointCommunicationTimes() {
        return this.countingMemoryController.getPointToPointCosts();
    }

    public List<List<Integer>> getRowBroadcastCommunicationTimes() {
        return this.countingMemoryController.getRowBroadcastCounts();
    }

    public List<List<Integer>> getColBroadcastCommunicationTimes() {
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
