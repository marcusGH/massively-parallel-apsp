package timingAnalysis;

import graphReader.GraphReader;
import matrixMultiplication.GeneralisedFoxOtto;
import memoryModel.CommunicationChannelException;
import timingAnalysis.topology.SquareGridTopology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import timingAnalysis.testWorkers.TestWorker1;
import timingAnalysis.testWorkers.TestWorker2;
import util.LoggerFormatter;
import util.Matrix;
import work.*;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TimedCommunicationManagerTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final MultiprocessorAttributes dummyFastProcessor = new MultiprocessorAttributes(
            100_000_000_000.0, 0, 0, 800000, 800000);
    private static final MultiprocessorAttributes slowSender = new MultiprocessorAttributes(
            10, 10, 15, 40, 1000000
    );

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.INFO);
    }

    /**
     * We have 3 x 3 workers that run for 2 phases:
     *   . . A
     *   D . B
     *   . . C
     *
     *  In the all phases, we do point to point sending A -> C -> B -> A.
     *  In all phases, D uses row broadcasting that B receives.
     *
     *  In the first phase, D will sleep for 4 seconds and C will sleep for 3 seconds.
     *  In the second phase, only C will sleep and for 2 seconds.
     *  The rest of the workers sleep for .1 seconds.
     */
    @Test
    void workersAreStalledCorrectly() {
        // SETUP
        TimedManager timedManager;
        try {
            Manager m = new Manager(3, 2, null, TestWorker1.class);
            timedManager = new TimedManager(m, dummyFastProcessor, SquareGridTopology::new);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Could not create managers");
            return;
        }

        // ACT
        try {
            timedManager.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("Manager failed to complete execution");
            return;
        }

        TimingAnalysisResult result = timedManager.getTimingAnalysisResult();

        System.out.println(result.getTotalExecutionTimes());
        System.out.println("---");
        System.out.println(result.getComputationTimes());
        System.out.println("---");
        System.out.println(result.getSendTimes());
        System.out.println("---");
        System.out.println(result.getStallTimes());

        // ASSERT
        /*
         * B should stall for about 4 seconds because of D, so during the second phase, A should need to stall for
         * about 4 seconds. In the second phase, B should only stall for 1 second because it's already 1 second behind C.
         * No other workers should stall.
         */
        Matrix<Double> stallTime = result.getStallTimes();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 2 && i != 2) continue;
                assertEquals(stallTime.get(i, j), 0, 1E-8, "This PE should not stall");
            }
        }
        assertEquals(stallTime.get(1, 2) * 1E-9, 4 + 1, 0.3, "Worker B stalls 5 seconds");
        assertEquals(stallTime.get(0, 2) * 1E-9, 4, 0.3, "Worker A stalls 4 seconds");

        // the dot workers should finish in .2 seconds
        Matrix<Double> totalTime = result.getTotalExecutionTimes();
        assertEquals(totalTime.get(0, 0) * 1E-9, 0.2, 0.01);
        assertEquals(totalTime.get(2, 0) * 1E-9, 0.2, 0.01);
        assertEquals(totalTime.get(0, 1) * 1E-9, 0.2, 0.01);
        assertEquals(totalTime.get(1, 1) * 1E-9, 0.2, 0.01);
        assertEquals(totalTime.get(2, 1) * 1E-9, 0.2, 0.01);
    }

    /**
     * We have 3 x 3 workers:
     *   A B C
     *   D . .
     *   . . E
     *
     *  In the all phases, we do point to point sending A -> D, B -> C, C -> A
     *  In all phases, D uses row broadcasting and E does column broadcasting.
     *
     *  The number of sends is [1, 3, 6]
     */
    @Test
    void workersHaveCorrectSendTimes() {
        // SETUP
        TimedManager timedManager;
        try {
            Manager m = new Manager(3, 3, null, TestWorker2.class);
            timedManager = new TimedManager(m, slowSender, SquareGridTopology::new);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Could not create managers");
            return;
        }

        // ACT
        try {
            timedManager.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("Manager failed to complete execution");
            return;
        }
        TimingAnalysisResult result = timedManager.getTimingAnalysisResult();

        System.out.println(result.getTotalExecutionTimes());
        System.out.println("---");
        System.out.println(result.getComputationTimes());
        System.out.println("---");
        System.out.println(result.getSendTimes());
        System.out.println("---");
        System.out.println(result.getStallTimes());

        // ASSERT
        Matrix<Double> sendTimes = result.getSendTimes();
        // it should take 1.5 seconds per broadcast, regardless of how much we send
        assertEquals(4.5, sendTimes.get(1, 0) * 1E-9, 0.01);
        assertEquals(4.5, sendTimes.get(2, 2) * 1E-9, 0.01);
        // we send 10 items in total, so send 40 bytes, i.e. takes 1 cycle in total, 0.1 seconds used on bandwidth
        // we send the data 3 times in 3 different communication phases, so also latency * 3 = 3 seconds
        assertEquals(3.1, sendTimes.get(0, 0) * 1E-9, 0.01);
        assertEquals(3.1, sendTimes.get(0, 1) * 1E-9, 0.01);
        assertEquals(3.1, sendTimes.get(0, 2) * 1E-9, 0.01);
    }

    @Test
    void summedTimingsAreCorrect() {
        // SETUP
        TimedRepeatedMatrixSquaring solver;
        try {
            GraphReader graphReader = new GraphReader("../test-datasets/cal-compressed-random-graphs/50.cedge", false);
            solver = new TimedRepeatedMatrixSquaring(graphReader, 4, SquareGridTopology::new, new MultiprocessorAttributes(), GeneralisedFoxOtto.class, 10);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Could not read graph");
            return;
        }

        // ACT
        solver.solve();
        TimingAnalysisResult result = solver.getTimingAnalysisResults();
        // individual timings
        Matrix<Double> send = result.getSendTimes();
        Matrix<Double> stall = result.getStallTimes();
        Matrix<Double> compute = result.getComputationTimes();
        // aggregate timings
        Matrix<Double> totalCommunication = result.getTotalCommunicationTimes();
        Matrix<Double> totalExecution = result.getTotalExecutionTimes();

        // ASSERT
        for (int i = 0; i < send.size(); i++) {
            for (int j = 0; j < send.size(); j++) {
                assertEquals(totalCommunication.get(i, j), send.get(i, j) + stall.get(i, j), 1,
                        String.format("The communication time aggregate for PE(%d, %d) is correct", i, j));
                assertEquals(totalExecution.get(i, j), compute.get(i, j) + totalCommunication.get(i, j), 1,
                        String.format("The execution time aggregate for PE(%d, %d) is correct", i, j));
            }
        }
    }

}
