package timingAnalysis;

import APSPSolver.RepeatedMatrixSquaring;
import graphReader.GraphCompressor;
import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import matrixMultiplication.GeneralisedFoxOtto;
import matrixMultiplication.MinPlusProduct;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;

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
    private final MultiprocessorAttributes multiprocessorAttributes;

    /**
     * Performs the same functionality as {@link RepeatedMatrixSquaring}, but with additional timing functionality.
     * This driver will decorate the {@link memoryModel.MemoryController}, {@link work.Manager}, and all the
     * {@link work.Worker}s such that during execution, the computation time of the workers are measured and the
     * time required to handle the communication between them is estimated. After {@link TimedRepeatedMatrixSquaring#solve()}
     * is called, the timing analyses can be retrieved with {@link TimedRepeatedMatrixSquaring#getTimingAnalysisResults()}.
     *
     * @param graphReader Container for the input graph of that APSP should be solved for. Let the number of nodes be n.
     * @param p The processing element lattice dimension. Regardless of n, a {@code p x p} lattice of processing elements
     *          will be used to solve the APSP problem, distributing work accordingly.
     * @param topologyFunction a constructor for a {@link Topology} class, indicating the memory topology of the processing elements
     * @param multiprocessorAttributes A specification of the multiprocessor hardware. Constants specified in this object
     *                                 will be used when estimating the communication time required by the algorithm.
     * @param minPlusProductImplementation An implementation of {@link MinPlusProduct}. There is also a requirement
     *                                     for the implementation to comply with the conditions described in
     *                                     {@link FoxOtto}. Otherwise, the algorithm will not work correctly.
     * @param numRepetitionsPerPhase The number of times to run each {@link work.Worker#computation(int)} when
     *                               measuring the computation time. The average of all these runs will be used.
     */
    public TimedRepeatedMatrixSquaring(GraphReader graphReader, int p, Function<Integer, Topology> topologyFunction,
                                       MultiprocessorAttributes multiprocessorAttributes,
                                       Class<? extends MinPlusProduct> minPlusProductImplementation, int numRepetitionsPerPhase) {
        super(graphReader, p, minPlusProductImplementation);
        this.numRepetitionsPerPhase = numRepetitionsPerPhase;
        this.topologyFunction = topologyFunction;
        this.multiprocessorAttributes = multiprocessorAttributes;
    }

    public TimedRepeatedMatrixSquaring(GraphReader graphReader, int p, Function<Integer, Topology> topologyFunction,
                                       Class<? extends MinPlusProduct> minPlusProductImplementation, int numRepetitionsPerPhase) {
        this(graphReader, p, topologyFunction, new MultiprocessorAttributes(), minPlusProductImplementation, numRepetitionsPerPhase);
    }

    public TimedRepeatedMatrixSquaring(GraphReader graphReader, Function<Integer, Topology> topologyFunction,
                                       Class<? extends MinPlusProduct> minPlusProductImplementation, int numRepetitionsPerPhase) {
        this(graphReader, graphReader.getNumberOfNodes(), topologyFunction, minPlusProductImplementation, numRepetitionsPerPhase);
    }

    @Override
    public void solve() {
        // prepare the initial memory content
        Map<String, Matrix<Number>> initialMemory = this.prepareInitialMemory();

        // create the timed manager
        try {
            Manager manager = new Manager(this.n, this.p, this.p, initialMemory, this.minPlusProductImplementation);
            this.timedManager = new TimedManager(manager, this.multiprocessorAttributes, this.topologyFunction);
            this.timedManager.enableFoxOttoTimeAveraging(this.numRepetitionsPerPhase);
        } catch (WorkerInstantiationException e) {
            System.err.println("The solver was not able to complete: ");
            e.printStackTrace();
            return;
        }

        this.manageWork(this.timedManager);
    }

    public TimingAnalysisResult getTimingAnalysisResults() {
        return this.timedManager.getTimingAnalysisResult();
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

        TimedRepeatedMatrixSquaring solver = new TimedRepeatedMatrixSquaring(graphReader,
                4, SquareGridTopology::new, new MultiprocessorAttributes(),
                GeneralisedFoxOtto.class, 100);
        solver.solve();

        TimingAnalysisResult timingResult = solver.getTimingAnalysisResults();

        System.out.println("Computation time:\n" + timingResult.getComputationTimes());
        System.out.println("Communication time:\n" + timingResult.getTotalCommunicationTimes());

        // TODO: Email overseers, asking about the success criteria regarding communication cost. Ok to discuss
        //       why might not be that good when mapping onto to actual hardware with values, but still minimize number
        //       of messages sent.

        // TODO: Consider the case under different models of computation? SIMD vs MIMD

        // TODO: Even if use data centre approach, looks like the ratio will be very poor as 2us is a lot even if
        //       do 4 or 16 phases between each communication (still under 50% computation time).

        // TODO: Is the repeated for-loop and averaging too unfair? I.e. do we store the numbers in registers or something,
        //       causing memory lookup to be way to fast? Might be more fair to do the computation once, and then again
        //       with the timing -> Can also be justified!

//        TimingAnalyser timingAnalyser = solver.getTimings();
//        try {
//            timingAnalyser.saveTimings("../evaluation/timing-data/" + filename);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // TODO TODO TODO: fix all of this

    }
}
