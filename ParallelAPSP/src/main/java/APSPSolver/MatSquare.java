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

public class MatSquare extends APSPSolver {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    protected final int n;
    protected final int p;
    protected final Class<? extends MinPlusProduct> minPlusProductImplementation;
    protected Matrix<Number> distanceMatrix;
    protected Matrix<Number> predecessorMatrix;

    /**
     * Creates a MatSquare solver, where p x p processing elements are used
     *
     * @param graphReader input graph
     * @param p the problem will be solves by p x p processing elements
     * @param minPlusProductImplementation procedure to perform the distance product and predecessor matrix computation
     */
    public MatSquare(GraphReader graphReader, int p,
                     Class<? extends MinPlusProduct> minPlusProductImplementation) {
        super(graphReader);
        this.p = p;
        // problem size can be distributed nicely
        if (super.n % p == 0) {
            this.n = super.n;
        } else {
            // add a few extra nodes, but no more than p new nodes
            this.n = super.n + (p - super.n % p);
        }
        this.minPlusProductImplementation = minPlusProductImplementation;
    }

    /**
     * Creates a non-general MatSquare solver. The number of processing elements will match the problem size
     * @param graphReader input graph
     * @param minPlusProductImplementation procedure to perform the distance product and predecessor matrix computation
     */
    public MatSquare(GraphReader graphReader,
                     Class<? extends MinPlusProduct> minPlusProductImplementation) {
        this(graphReader, graphReader.getNumberOfNodes(), minPlusProductImplementation);
    }

    protected Map<String, Matrix<Number>> prepareInitialMemory() {
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();

        Matrix<Number> originalAdjMatrix = this.graph.getAdjacencyMatrix();
        // The entry D[i, j] contains the weight of edge (i, j), defaulting to \infty if there is no such edge
        Matrix<Number> distMatrix = new Matrix<>(this.n);
        // The entry P[i, j] contains the predecessor of j in the shortest path i -~-> j, defaulting to value j
        //   if no such path has been found yet
        Matrix<Number> predMatrix = new Matrix<>(this.n);
        for (int i = 0; i < this.n; i++) {
            for (int j = 0; j < this.n; j++) {
                // self-loop
                if (i == j) {
                    distMatrix.set(i, j, 0);
                }
                // from original adjacency matrix
                else if (i < super.n && j < super.n) {
                    distMatrix.set(i, j, originalAdjMatrix.get(i, j));
                }
                // dummy node
                else {
                    distMatrix.set(i, j, Double.POSITIVE_INFINITY);
                }

                // setup the predecessor matrix as well
                if (this.graph.hasEdge(i, j)) {
                    predMatrix.set(i, j, i);
                } else {
                    predMatrix.set(i, j, j);
                }
            }
        }

        // We want to square the weight matrix, so input it as both "A" and "B"
        initialMemory.put("A", distMatrix);
        initialMemory.put("B", distMatrix);
        initialMemory.put("P", predMatrix);

        // log initial conditions
        LOGGER.fine("Distance matrix before start:\n" + distMatrix);
        LOGGER.fine("Pred matrix before start:\n" + predMatrix);

        return initialMemory;
    }


    @Override
    public void solve() {
        // prepare the initial memory content
        Map<String, Matrix<Number>> initialMemory = this.prepareInitialMemory();

        // create the manager
        Manager manager;
        try {
            manager = new Manager(this.n, this.p, this.p, initialMemory, this.minPlusProductImplementation);
        } catch (WorkerInstantiationException e) {
            System.err.println("The solver was not able to complete: ");
            e.printStackTrace();
            return;
        }

        this.manageWork(manager);
    }

    protected void manageWork(Manager manager) {
        // we store our results here
        Matrix<Number> distMatrix = null;
        Matrix<Number> predMatrix = null;

        // repeatedly square the distance- and predecessor matrix with min-plus product
        int numIterations = (int) Math.ceil(Math.log(this.n) / Math.log(2));
        LOGGER.info("The graph size is " + this.n + " so " + numIterations + " MinPlusProduct iterations are required.");
        for (int i = 0; i < numIterations; i++) {
            LOGGER.info("Starting Manager to square the matrix (iteration " + i + " / " + numIterations + ")");
            // run the algorithm
            try {
                manager.doWork();
            } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
                System.err.println("The solver encountered an error during execution: ");
                e.printStackTrace();
                return;
            }

            // Read output after final iteration
            if (i == numIterations - 1) {
                distMatrix = manager.getResult("dist");
                LOGGER.fine("Distance matrix at iteration " + i + " is:\n" + distMatrix);
                predMatrix = manager.getResult("pred", true);
                LOGGER.fine("Pred matrix are iteration " + i + " is:\n" + predMatrix);
            }
            // This statement is not required because at the end of FoxOtto, the memory will already
            //   be in the correct positions for the next execution of FoxOtto
//            manager.setPrivateMemory(Map.of("A", distMatrix, "B", distMatrix, "P", predMatrix));
        }

        // log and save
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
            } else if (path.size() > this.n) {
                throw new IllegalStateException(String.format("Encountered infinite loop in path from %d to %d", i, j));
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

        APSPSolver solver = new MatSquare(graphReader, FoxOtto.class);
        solver.solve();

        System.out.println(solver.getShortestPath(0, 4));
        System.out.println(solver.getDistanceFrom(0, 4));
    }
}
