package APSPSolver;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import matrixMultiplication.MinPlusProduct;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerFactory;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepeatedMatrixSquaring extends APSPSolver {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final Class<? extends MinPlusProduct> minPlusProductImplementation;
    private final GraphReader graphReader;
    private Matrix<Number> distanceMatrix;
    private Matrix<Number> predecessorMatrix;

    // TODO: use graphreader object in parent as well!

    public RepeatedMatrixSquaring(Matrix<? extends Number> adjacencyMatrix, GraphReader graphReader,
                                  Class<? extends MinPlusProduct> minPlusProductImplementation) {
        super(adjacencyMatrix);
        this.graphReader = graphReader;
        this.minPlusProductImplementation = minPlusProductImplementation;
    }

    @Override
    public void solve() {
        // prepare the initial memory content
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        // We want to square the weight matrix, so input it as both "A" and "B"
        Matrix<Number> distMatrix = this.adjacencyMatrix;
        initialMemory.put("A", distMatrix);
        initialMemory.put("B", distMatrix);
        // The entry P[i, j] contains the predecessor of j in the shortest path i -~-> j, defaulting to value j
        //   if no such path has been found yet
        Matrix<Number> predMatrix = new Matrix<>(this.n);
        for (int i = 0; i < this.n; i++) {
            for (int j = 0; j < this.n; j++) {
                if (this.graphReader.hasEdge(i, j)) {
                    predMatrix.set(i, j, i);
                } else {
                    predMatrix.set(i, j, j);
                }
            }
        }
        initialMemory.put("P", predMatrix);

        // create the manager
        WorkerFactory workerFactory;
        Manager manager;
        try {
          manager = new Manager(this.n, this.n, initialMemory, SquareGridTopology::new, this.minPlusProductImplementation);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            return;
        }

        // repeatedly square the distance- and predecessor matrix with min-plus product
        int numIterations = (int) Math.ceil(Math.log(this.n) / Math.log(2));
        for (int i = 0; i < numIterations; i++) {
            System.out.println("===");
            System.out.println(distMatrix); // debug

            // run the algorithm
            try {
                manager.doWork();
            } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
                e.printStackTrace();
                return;
            }

            // prepare for the next iteration by updating the input to what the result from the previous iteration was
            distMatrix = manager.getResult("dist");
            predMatrix = manager.getResult("pred");
            manager.resetMemory(Map.of("A", distMatrix, "B", distMatrix, "P", predMatrix));
        }
        System.out.println("===");
        System.out.println(distMatrix);

        this.predecessorMatrix = predMatrix;
        this.distanceMatrix = distMatrix;
    }

    public Optional<List<Integer>> getShortestPath(int i, int j) {
        if (this.predecessorMatrix == null) {
            throw new IllegalStateException("Solve must be called before querying shortest path");
        }
        Deque<Integer> path = new LinkedList<>();

        if (this.predecessorMatrix.get(i, j).intValue() == j) {
            return Optional.empty();
        }

        do {
            int pred = this.predecessorMatrix.get(i, j).intValue();
            assert pred != j;
            path.addFirst(j);
            j = pred;
        } while (i !=  j);
        path.addFirst(i);
        return Optional.of(new ArrayList<>(path));
    }

    public static void main(String[] args) {
        LoggerFormatter.setupLogger(LOGGER, Level.INFO);

        GraphReader graphReader;
        Matrix<Double> adjacencyMatrix;
        try {
            graphReader = new GraphReader("../datasets/7-node-example.cedge");
            adjacencyMatrix = graphReader.getAdjacencyMatrix(true);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        APSPSolver solver = new RepeatedMatrixSquaring(adjacencyMatrix, graphReader, FoxOtto.class);
        solver.solve();

        System.out.println(solver.getShortestPath(0, 2));
    }
}
