package timingAnalysis;

import matrixMultiplication.FoxOtto;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TimingAnalysisTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final int INF = Integer.MAX_VALUE;

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.FINE);
    }

//    @Test
//    void timingManagerProducesCorrectResult() {
//
//        // SETUP
//
//        // the graph we're working with
//        Number[][] adjacencyGrid = {
//                {0,   6,    2,   3,  INF, INF, INF},
//                {INF, 0  , INF, INF,  1 , INF, INF},
//                {INF, INF, 0  , INF, INF,  2,   1 },
//                {INF, INF, INF,  0 , INF, INF,  2 },
//                {INF, INF, INF, INF,  0 , INF, INF},
//                {INF,  1 , INF, INF, INF,  0 , INF},
//                {INF, INF, INF, INF, INF, INF,  0 },
//        };
//        Matrix<Number> adjMatrix = new Matrix<Number>(7, adjacencyGrid);
//        // setup the predecessor matrix
//        Matrix<Number> predMatrix = new Matrix<>(7);
//        for (int i = 0; i < 7; i++) {
//            for (int j = 0; j < 7; j++) {
//                if (INF == adjMatrix.get(i, j).intValue()) {
//                    predMatrix.set(i, j, j);
//                } else {
//                    predMatrix.set(i, j, i);
//                }
//
//            }
//        }
//        // and put the two as initial memory content
//        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
//        initialMemory.put("A", adjMatrix);
//        initialMemory.put("B", adjMatrix);
//        initialMemory.put("P", predMatrix);
//
//        // create the timing manager
//        TimedManager timedManager;
//        try {
//            Manager manager = new Manager(7, 7, initialMemory, FoxOtto.class);
//            timedManager = new TimedManager(manager, SquareGridTopology::new);
//        } catch (WorkerInstantiationException e) {
//            e.printStackTrace();
//            fail("The timed manager could not be constructed correctly");
//            return;
//        }
//
//        // ACT
//
//        Matrix<Number> distResult;
//        Matrix<Number> predResult;
//
//        Matrix<Long> computeTimes;
//        List<Matrix<Integer>> pointCounts;
//        List<List<Integer>> rowCounts;
//        List<List<Integer>> colCounts;
//        try {
//            // compute the next matrix
//            timedManager.doWork();
//
//            // and extract the results
//            distResult = timedManager.getResult("dist", true);
//            predResult = timedManager.getResult("pred", true);
//            // and the timing analysis
//            computeTimes = timedManager.getComputationTimes();
//            pointCounts =  timedManager.getPointToPointCommunicationTimes();
//            rowCounts = timedManager.getRowBroadcastCommunicationTimes();
//            colCounts = timedManager.getColBroadcastCommunicationTimes();
//        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
//            e.printStackTrace();
//            fail("The timed manager failed to complete its work");
//            return;
//        }
//
//        // ASSERT
//        Number[][] expectedDist = {
//            { 0, 6, 2, 3, 7, 4, 3 },
//            { INF, 0, INF, INF, 1, INF, INF },
//            { INF, 3, 0, INF, INF, 2, 1 },
//            { INF, INF, INF, 0, INF, INF, 2 },
//            { INF, INF, INF, INF, 0, INF, INF },
//            { INF, 1, INF, INF, 2, 0, INF },
//            { INF, INF, INF, INF, INF, INF, 0 }};
//        Number[][] expectedPred = {
//            { 0, 0, 0, 0, 1, 2, 2 },
//            { 0, 1, 2, 3, 1, 5, 6 },
//            { 0, 5, 2, 3, 4, 2, 2 },
//            { 0, 1, 2, 3, 4, 5, 3 },
//            { 0, 1, 2, 3, 4, 5, 6 },
//            { 0, 5, 2, 3, 1, 5, 6 },
//            { 0, 1, 2, 3, 4, 5, 6 }};
//
//        // check the computation
//        assertEquals(new Matrix<>(7, expectedDist), distResult,
//                "The timing manager produces the correct distance matrix");
//        assertEquals(new Matrix<>(7, expectedPred), predResult,
//                "The timing manager produces the correct distance matrix");
//
//        // check that some timing was done
//        assertEquals(7, computeTimes.size(),  "There were 7 x 7 computation time measures");
//        assertEquals(14, pointCounts.size(), "There were twice as many communication phases as computation phases");
//        assertEquals(14, rowCounts.size(), "There were 14 broadcasting opportunities");
//        assertEquals(14, colCounts.size(), "There were 14 broadcasting opportunities");
//
//        for (int i = 0; i < 7; i++) {
//            for (int j = 0; j < 7; j++) {
//                assertTrue(computeTimes.get(i, j) > 0.0, "Some computation time measured for PE(" + i + ", " + j + ").");
//            }
//        }
//        assertTrue(pointCounts.get(1).get(5, 4) > 0, "Some point-to-point communication happened in communication_after");
//        assertTrue(rowCounts.get(6).get(4) > 0, "Row broadcasting happened in communication_before");
//        assertEquals(colCounts.get(6).get(4), 0, "No column broadcasting happened in communication_before");
//        assertEquals(rowCounts.get(5).get(4), 0, "No row broadcasting happened in communication_after");
//
//        // and print the results just to sanity check
//        System.out.println("Point-to-point counts:");
//        System.out.println(pointCounts.get(3));
//        System.out.println("Computation timings:");
//        System.out.println(computeTimes);
//        System.out.println("Row broadcast usage:");
//        System.out.println(rowCounts.get(4));
//    }

    // TODO: write test where each worker sends different amount of data, and it goes over longer distances
}