package timingAnalysis;

import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import timingAnalysis.testWorkers.TestWorker1;
import timingAnalysis.testWorkers.TestWorker2;
import util.LoggerFormatter;
import util.Matrix;
import work.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TimingAnalysisMemoryControllerTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final MultiprocessorAttributes dummyFastProcessor = new MultiprocessorAttributes(
            100_000_000_000.0, 0, 0, 800000, 800000);
    private static final MultiprocessorAttributes slowSender = new MultiprocessorAttributes(
            10, 10, 15, 40, 1000000
    );

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.FINE);
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

        System.out.println(timedManager.getTotalExecutionTimes());
        System.out.println("---");
        System.out.println(timedManager.getComputationTimes());
        System.out.println("---");
        System.out.println(timedManager.getSendTimes());
        System.out.println("---");
        System.out.println(timedManager.getStallTimes());

        // ASSERT
        /*
         * B should stall for about 4 seconds because of D, so during the second phase, A should need to stall for
         * about 4 seconds. In the second phase, B should only stall for 1 second because it's already 1 second behind C.
         * No other workers should stall.
         */
        Matrix<Double> stallTime = timedManager.getStallTimes();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 2 && i != 2) continue;
                assertEquals(stallTime.get(i, j), 0, 1E-8, "This PE should not stall");
            }
        }
        assertEquals(stallTime.get(1, 2) * 1E-9, 4 + 1, 0.3, "Worker B stalls 5 seconds");
        assertEquals(stallTime.get(0, 2) * 1E-9, 4, 0.3, "Worker A stalls 4 seconds");

        // the dot workers should finish in .2 seconds
        Matrix<Double> totalTime = timedManager.getTotalExecutionTimes();
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

        System.out.println(timedManager.getTotalExecutionTimes());
        System.out.println("---");
        System.out.println(timedManager.getComputationTimes());
        System.out.println("---");
        System.out.println(timedManager.getSendTimes());
        System.out.println("---");
        System.out.println(timedManager.getStallTimes());

        // ASSERT
        Matrix<Double> sendTimes = timedManager.getSendTimes();
        // it should take 1.5 seconds per broadcast, regardless of how much we send
        assertEquals(4.5, sendTimes.get(1, 0) * 1E-9, 0.01);
        assertEquals(4.5, sendTimes.get(2, 2) * 1E-9, 0.01);
        // we send 10 items in total, so send 40 bytes, i.e. takes 1 cycle in total, 0.1 seconds used on bandwidth
        // we send the data 3 times in 3 different communication phases, so also latency * 3 = 3 seconds
        assertEquals(3.1, sendTimes.get(0, 0) * 1E-9, 0.01);
        assertEquals(3.1, sendTimes.get(0, 1) * 1E-9, 0.01);
        assertEquals(3.1, sendTimes.get(0, 2) * 1E-9, 0.01);
    }

}
