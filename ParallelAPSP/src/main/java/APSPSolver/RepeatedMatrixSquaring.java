package APSPSolver;

import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import matrixMultiplication.MinPlusProduct;
import memoryModel.CommunicationChannelException;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepeatedMatrixSquaring extends APSPSolver {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final Class<? extends MinPlusProduct> minPlusProductImplementation;
    private Matrix<Number> distanceMatrix;
    private Matrix<Number> predecessorMatrix;

    public RepeatedMatrixSquaring(GraphReader graphReader,
                                  Class<? extends MinPlusProduct> minPlusProductImplementation) {
        super(graphReader);
        this.minPlusProductImplementation = minPlusProductImplementation;
    }

    @Override
    public void solve() {
        // prepare the initial memory content
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        // We want to square the weight matrix, so input it as both "A" and "B"
        Matrix<Number> distMatrix = this.graph.getAdjacencyMatrix();
        for (int i = 0; i < this.n; i++) {
            distMatrix.set(i, i, 0);
        }
        initialMemory.put("A", distMatrix);
        initialMemory.put("B", distMatrix);
        // The entry P[i, j] contains the predecessor of j in the shortest path i -~-> j, defaulting to value j
        //   if no such path has been found yet
        Matrix<Number> predMatrix = new Matrix<>(this.n);
        for (int i = 0; i < this.n; i++) {
            for (int j = 0; j < this.n; j++) {
                if (this.graph.hasEdge(i, j)) {
                    predMatrix.set(i, j, i);
                } else {
                    predMatrix.set(i, j, j);
                }
            }
        }
        initialMemory.put("P", predMatrix);

        // create the manager
        Manager manager;
        try {
            manager = new Manager(this.n, this.n, initialMemory, this.minPlusProductImplementation);
        } catch (WorkerInstantiationException e) {
            System.err.println("The solver was not able to complete: ");
            e.printStackTrace();
            return;
        }

        // repeatedly square the distance- and predecessor matrix with min-plus product
        int numIterations = (int) Math.ceil(Math.log(this.n) / Math.log(2));
        for (int i = 0; i < numIterations; i++) {
            // run the algorithm
            try {
                manager.doWork();
            } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
                System.err.println("The solver encountered an error during execution: ");
                e.printStackTrace();
                return;
            }

            // prepare for the next iteration by updating the input to what the result from the previous iteration was
            distMatrix = manager.getResult("dist");
            System.out.println("DIstance:\n" + distMatrix);
            predMatrix = manager.getResult("pred", true);
            manager.resetMemory(Map.of("A", distMatrix, "B", distMatrix, "P", predMatrix));
        }
        LOGGER.log(Level.FINE, "The computed distance matrix is:\n" + distMatrix);
        LOGGER.log(Level.FINE, "The computed predecessor matrix is:\n" + predMatrix);
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
            if (pred == j) {
                throw new IllegalStateException(String.format("The predecessor matrix should not have self-references: Pred(%d, %d)=%d", i, j, pred));
            }
            path.addFirst(j);
            j = pred;
        } while (i !=  j);
        path.addFirst(i);
        return Optional.of(new ArrayList<>(path));
    }

    @Override
    public Number getDistanceFrom(int i, int j) {
        if (this.distanceMatrix == null) {
            throw new IllegalStateException("Solve must be called before querying distance between nodes");
        }
        return this.distanceMatrix.get(i, j);
    }

    public static void main(String[] args) {
        LoggerFormatter.setupLogger(LOGGER, Level.FINE);

        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../datasets/9-node-example.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        APSPSolver solver = new RepeatedMatrixSquaring(graphReader, FoxOtto.class);
        solver.solve();

        System.out.println(solver.getShortestPath(0, 4));
        System.out.println(solver.getDistanceFrom(0, 4));
    }
}
