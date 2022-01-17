package work;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import util.Matrix;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TimedManager extends Manager {

    private final Matrix<TimedWorker> timedWorkers;

    public TimedManager(Manager manager, Function<Integer, ? extends Topology> memoryTopology) {
        super(manager);
        // decorate all the workers
        this.timedWorkers = new Matrix<>(this.p);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                TimedWorker tw = new TimedWorker(this.workers.get(i, j));
                this.timedWorkers.set(i, j, tw);
                // use dynamic dispatch so that we don't need to reimplement doWork etc.
                this.workers.set(i, j, tw);
            }
        }
        // TODO: decorate the memory controller
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
        TimedManager timedManager = new TimedManager(manager, SquareGridTopology::new);

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
        }
    }
}
