package main;

import APSPSolver.APSPSolver;
import APSPSolver.RepeatedMatrixSquaring;
import graphReader.GraphCompressor;
import graphReader.GraphReader;
import matrixMultiplication.GeneralisedFoxOtto;
import matrixMultiplication.MinPlusProduct;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import org.junit.jupiter.api.BeforeAll;
import timingAnalysis.MultiprocessorAttributes;
import timingAnalysis.TimedRepeatedMatrixSquaring;
import util.LoggerFormatter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Evaluation {

    private static final String RANDOM_GRAPH_PATH = "../test-datasets/cal-compressed-random-graphs";
    private static final String RESULT_SAVE_PATH = "../evaluation/timing-data";
    private static final int AVG_REPETITIONS = 5;
    private static final Function<Integer, Topology> TOPOLOGY = SquareGridTopology::new;
    private static final Class<? extends MinPlusProduct> FOXOTTO = GeneralisedFoxOtto.class;
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.INFO);
    }

    public MultiprocessorAttributes getSandyBridgeAttributes() {
        return new MultiprocessorAttributes(MultiprocessorAttributes.ACER_NITRO_CPU_CYCLES_PER_SECOND,
                MultiprocessorAttributes.SANDY_BRIDGE_CACHE_LATENCY_SEND,
                MultiprocessorAttributes.SANDY_BRIDGE_CACHE_LATENCY_SEND,
                MultiprocessorAttributes.SANDY_BRIDGE_INTERCONNECT_BANDWIDTH,
                MultiprocessorAttributes.SANDY_BRIDGE_INTERCONNECT_BANDWIDTH);
    }

    public MultiprocessorAttributes getSunwayLightAttributes() {
        // The MPE/CPE chip is connected via a network-on-chip (NoC) and the system interface (SI) is
        // used to connect the system outside of the node. The SI is a standard PCIe interface. The
        // bidirectional bandwidth is 16 GB/s with a latency around 1 us.

        // we only send in one direction, so use at most half the bandwidth available at most
        double bytes_in_GB = Math.pow(2, 30); // 1_073_741_824
        return new MultiprocessorAttributes(1E-6, 1E-6,
                8 * bytes_in_GB, 8 * bytes_in_GB);
    }

    public MultiprocessorAttributes getInternetAttributes() {
        // Latency: 30ms or less for regional round trips within Europe.
        // Bandwidth: The regional average speed of 90.56Mbps makes it the fastest of the 13 global regions overall.0
        double bytes_in_Mb = Math.pow(2, 20) / 8; // Megabit / 8 = MegaByte
        double latency = 30 * 1E-3; // 30 ms
        double bandwidth = 90 * bytes_in_Mb;
        return new MultiprocessorAttributes(latency, latency, bandwidth, bandwidth);

    }

    public GraphReader getGraph(int size) throws ParseException {
        return new GraphReader(String.format("%s/%d.cedge", RANDOM_GRAPH_PATH, size), false);
    }

    /**
     * Uses randomly generated graphs of the sizes found in problemSizes
     * @param p a 2d grid of p x p processing elements will be used to solve the problem
     * @param problemSizes a list of problem sizes (in number of nodes)
     */
    public void measureScaling(int p, List<Integer> problemSizes, int numRepetitions) {
        int ITER_OFFSET = 0;

        // we use this one for now
        MultiprocessorAttributes multiprocessor = getInternetAttributes();
        for (int iter = ITER_OFFSET; iter < numRepetitions; iter++) {
            LOGGER.info(String.format(" === Evaluation is measuring scaling for repetition %d / %d ===\n", iter + 1, numRepetitions));
            for (int i : problemSizes) {
                LOGGER.info(String.format("Evaluation is measuring scaling for n=%d.", i));
                // fetch the input
                GraphReader graph;
                try {
                    graph = getGraph(i);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
                // create the solver
                TimedRepeatedMatrixSquaring solver = new TimedRepeatedMatrixSquaring(graph, p, TOPOLOGY, multiprocessor, FOXOTTO, AVG_REPETITIONS);

                solver.solve();

                // then save the result
                String filename = String.format("%s/cal-random-internet-3-repeats-n-%d-p-%d.%d.csv", RESULT_SAVE_PATH, i, p, iter);
                try {
                    solver.getTimingAnalysisResults().saveResult(filename);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public void measureCalRoadNetworkExecutionTimes(int p, int numRepetitions) {
        int ITER_OFFSET = 0;

        MultiprocessorAttributes multiprocessor = getSandyBridgeAttributes();
        GraphReader cal;
        try {
            cal = new GraphReader("../datasets/cal.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        // compress the graph
        GraphCompressor calCompressed = new GraphCompressor(cal);
        TimedRepeatedMatrixSquaring solver = new TimedRepeatedMatrixSquaring(calCompressed.getCompressedGraph(), p, TOPOLOGY, multiprocessor, FOXOTTO, AVG_REPETITIONS);

        for (int i = ITER_OFFSET; i < numRepetitions; i++) {
            LOGGER.info(String.format("\n === Evaluation is measuring california road execution time for repetition %d / %d ===\n", i + 1, numRepetitions));

            // solve
            solver.solve();

            // save the result
            String filename = String.format("%s/cal-real-sandy-bridge/cal-real-sandy-bridge-p-%d.%d.csv", RESULT_SAVE_PATH, p, i);
            try {
                solver.getTimingAnalysisResults().saveResult(filename);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void outputPathOnCaliforniaNetwork(int p, int startNode, int endNode) {
        GraphReader cal;
        try {
            cal = new GraphReader("../datasets/cal.cedge", false);
//            cal = new GraphReader("../test-datasets/cal-compressed-random-graphs/500.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        // compress the graph and use it as the solver
        GraphCompressor calCompressed = new GraphCompressor(cal, GraphCompressor.getCurriedFoxOttoAPSPSolverConstructor(p));
        APSPSolver solver = calCompressed;

        solver.solve();

        // Some interesting hand-picked example pairs:
        //   1 -> 19000,
        //   10585 -> 9000,
        //   14000 -> 13520,
        //   60 -> 200
        Integer[][] examplePairs = {
                {1, 19000},
                {10585, 9000},
                {14000, 13520},
                {60, 200},
        };
        for (int i = 0; i < 4; i++) {
            int start = examplePairs[i][0];
            int end = examplePairs[i][1];
            System.out.println(String.format("Path from %d to %d is length %f and consists of:",
                    start, end, solver.getDistanceFrom(start, end).doubleValue()));
            System.out.println(solver.getShortestPath(start, end));
        }
    }

    public static void main(String[] args) {
        Evaluation evaluation = new Evaluation();
        setupLogger();

//        List<Integer> ns = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
//                150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700);
//        List<Integer> ns = Arrays.asList(700);
//        evaluation.measureScaling(128, ns, 3);
//        evaluation.measureCalRoadNetworkExecutionTimes(128, 5);
        // TODO:
        //    serial for both cal and random

        evaluation.outputPathOnCaliforniaNetwork(8, 1, 10);
    }
}
