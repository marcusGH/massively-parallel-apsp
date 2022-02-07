package timingAnalysis;

import APSPSolver.RepeatedMatrixSquaring;
import graphReader.GraphCompressor;
import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import matrixMultiplication.MinPlusProduct;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimedRepeatedMatrixSquaring extends RepeatedMatrixSquaring {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final int numRepetitionsPerPhase;
    private final Function<Integer, Topology> topologyFunction;

    private TimedManager timedManager;

    public TimedRepeatedMatrixSquaring(GraphReader graphReader, Function<Integer, Topology> topologyFunction,
                                       Class<? extends MinPlusProduct> minPlusProductImplementation, int numRepetitionsPerPhase) {
        super(graphReader, minPlusProductImplementation);
        this.numRepetitionsPerPhase = numRepetitionsPerPhase;
        this.topologyFunction = topologyFunction;
    }

    @Override
    public void solve() {
        // prepare the initial memory content
        Map<String, Matrix<Number>> initialMemory = this.prepareInitialMemory();
        Matrix<Number> distMatrix = null;
        Matrix<Number> predMatrix = null;

        // create the manager
        try {
            Manager manager = new Manager(this.n, this.n, initialMemory, this.minPlusProductImplementation);
            this.timedManager = new TimedManager(manager, this.topologyFunction);
            this.timedManager.enableFoxOttoTimeAveraging(this.numRepetitionsPerPhase);
            // the communication is the same at each before and after phase, so only save one of each
            this.timedManager.disableCommunicationTrackingAfterNPhases(2);
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
                this.timedManager.doWork();
            } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
                System.err.println("The solver encountered an error during execution: ");
                e.printStackTrace();
                return;
            }

            // prepare for the next iteration by updating the input to what the result from the previous iteration was
            distMatrix = this.timedManager.getResult("dist");
            LOGGER.fine("Distance matrix at iteration " + i + " is:\n" + distMatrix);
            predMatrix = this.timedManager.getResult("pred", true);
            LOGGER.fine("Pred matrix are iteration " + i + " is:\n" + predMatrix);
            timedManager.setPrivateMemory(Map.of("A", distMatrix, "B", distMatrix, "P", predMatrix));
        }
        LOGGER.log(Level.FINE, "The computed distance matrix is:\n" + distMatrix);
        LOGGER.log(Level.FINE, "The computed predecessor matrix is:\n" + predMatrix);
        this.predecessorMatrix = predMatrix;
        this.distanceMatrix = distMatrix;
    }

    public TimingAnalyser getTimings() {
        // use the analyser
        TimingAnalyser timingAnalyser = new TimingAnalyser(this.timedManager, TimingAnalyser.ACER_NITRO_CPU_CYCLES_PER_NANOSECOND,
                TimingAnalyser.POINT_TO_POINT_SEND_CLOCK_CYCLES, TimingAnalyser.BROADCAST_CLOCK_CYCLES,
                64, 64);
//        timingAnalyser.getComputationTimes();
        return timingAnalyser;
    }

    public static void main(String[] args) {
        String filename = "SF-d-70";

        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/" + filename + ".cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        LoggerFormatter.setupLogger(LOGGER, Level.INFO);

        GraphCompressor graphCompressor = new GraphCompressor(graphReader);
        graphReader = graphCompressor.getGraphReader();

        TimedRepeatedMatrixSquaring solver = new TimedRepeatedMatrixSquaring(graphReader, SquareGridTopology::new,
                FoxOtto.class, 100);
        solver.solve();

        // TODO: Email overseers, asking about the success criteria regarding communication cost. Ok to discuss
        //       why might not be that good when mapping onto to actual hardware with values, but still minimize number
        //       of messages sent.

        // TODO: Consider the case under different models of computation? SIMD vs MIMD

        // TODO: Even if use data centre approach, looks like the ratio will be very poor as 2us is a lot even if
        //       do 4 or 16 phases between each communication (still under 50% computation time).

        // TODO: Is the repeated for-loop and averaging too unfair? I.e. do we store the numbers in registers or something,
        //       causing memory lookup to be way to fast? Might be more fair to do the computation once, and then again
        //       with the timing -> Can also be justified!

        TimingAnalyser timingAnalyser = solver.getTimings();
        try {
            timingAnalyser.saveTimings("../evaluation/timing-data/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
