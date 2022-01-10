package APSPSolver;

import graphReader.GraphReader;
import matrixMultiplication.BroadcastMatrixMultiplication;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerFactory;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepeatedMatrixSquaring extends APSPSolver {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public RepeatedMatrixSquaring(Matrix<? extends Number> adjacencyMatrix) {
        super(adjacencyMatrix);
    }

    @Override
    public void solve() {
        // create the manager
        WorkerFactory workerFactory;
        Manager manager;
        try {
          workerFactory = new WorkerFactory(BroadcastMatrixMultiplication.class);
          manager = new Manager(this.n, this.n, null, SquareGridTopology::new, workerFactory);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            return;
        }

        // prepare the initial memory content
        Map<String, Matrix<Number>> initialMemory;
        Matrix<Number> squaredMatrix = this.adjacencyMatrix;


        int numIterations = (int) Math.ceil(Math.log(this.n) / Math.log(2));
        for (int i = 0; i < numIterations; i++) {
            System.out.println(squaredMatrix); // debug

            // setup the memory
            initialMemory = new HashMap<>();
            initialMemory.put("A", squaredMatrix);
            initialMemory.put("B", squaredMatrix);

            manager.resetMemory(initialMemory);
            try {
                // TODO: work from here jk. One task to do is to figure out why it gets stuck when it has done
                //       its computation. You should also look at the best way to let Manager do work repeatedly,
                //       and this may also be the reason it's getting stuck!
                manager.doWork();
            } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
                e.printStackTrace();
                return;
            }

            squaredMatrix = manager.getResult("C");
        }
    }

    public static void main(String[] args) {
        LoggerFormatter.setupLogger(LOGGER, Level.FINE);

        Matrix<Double> adjacencyMatrix;
        try {
            GraphReader graphReader = new GraphReader("../datasets/7-node-example.cedge");
            adjacencyMatrix = graphReader.getAdjacencyMatrix(true);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        APSPSolver solver = new RepeatedMatrixSquaring(adjacencyMatrix);
        solver.solve();
    }
}
