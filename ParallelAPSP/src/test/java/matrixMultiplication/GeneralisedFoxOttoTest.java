package matrixMultiplication;

import APSPSolver.APSPSolver;
import APSPSolver.RepeatedMatrixSquaring;
import graphReader.GraphReader;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import timingAnalysis.TimedManager;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class GeneralisedFoxOttoTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final int INF = Integer.MAX_VALUE;

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.FINE);
    }

    @Test
    void workerProducesCorrectMatrixForSimpleGraph() {
        // SETUP

        // the graph we're working with
        Number[][] adjacencyGrid = {
                {0,   6,    2,   3,  INF, INF, INF, INF},
                {INF, 0  , INF, INF,  1 , INF, INF, INF},
                {INF, INF, 0  , INF, INF,  2,   1,  INF},
                {INF, INF, INF,  0 , INF, INF,  2,  INF},
                {INF, INF, INF, INF,  0 , INF, INF, INF},
                {INF,  1 , INF, INF, INF,  0 , INF, INF},
                {INF, INF, INF, INF, INF, INF,  0 , INF},
                {INF, INF, INF, INF, INF, INF,  INF , 0},
        };
        Matrix<Number> adjMatrix = new Matrix<Number>(8, adjacencyGrid);
        // setup the predecessor matrix
        Matrix<Number> predMatrix = new Matrix<>(8);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (INF == adjMatrix.get(i, j).intValue()) {
                    predMatrix.set(i, j, j);
                } else {
                    predMatrix.set(i, j, i);
                }

            }
        }
        // and put the two as initial memory content
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        initialMemory.put("A", adjMatrix);
        initialMemory.put("B", adjMatrix);
        initialMemory.put("P", predMatrix);

        // create the manager
        Manager m;
        try {
            m = new Manager(8, 2,2, initialMemory, GeneralisedFoxOtto.class);
            m = new TimedManager(m, SquareGridTopology::new);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("The manager or worker factory could not be created");
            return;
        }

        // ACT
        Matrix<Number> distResult;
        Matrix<Number> predResult;
        try {
            // compute W2 and P2
            m.doWork();
            distResult = m.getResult("dist", true);
            predResult = m.getResult("pred", true);
            System.out.println(distResult);
            System.out.println(predResult);

            // compute W3 and P3
            m.setPrivateMemory(Map.of("A", distResult, "B", distResult, "P", predResult));
            m.doWork();
            distResult = m.getResult("dist", true);
            predResult = m.getResult("pred", true);
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("The workers could not successfully finish their work");
            return;
        }

        // ASSERT

        Number[][] expectedPred = {
            {0, 5, 0, 0, 1, 2, 2, 7},
            {0, 1, 2, 3, 1, 5, 6, 7},
            {0, 5, 2, 3, 1, 2, 2, 7},
            {0, 1, 2, 3, 4, 5, 3, 7},
            {0, 1, 2, 3, 4, 5, 6, 7},
            {0, 5, 2, 3, 1, 5, 6, 7},
            {0, 1, 2, 3, 4, 5, 6, 7},
            {0, 1, 2, 3, 4, 5, 6, 7},
        };
        Matrix<Number> expectedPredMatrix = new Matrix<>(8, expectedPred);
        Number[][] expectedDist = {
                {0,   5,    2,   3,  6, 4, 3, INF},
                {INF, 0  , INF, INF,  1 , INF, INF, INF},
                {INF, 3, 0  , INF, 4,  2,   1, INF },
                {INF, INF, INF,  0 , INF, INF,  2, INF },
                {INF, INF, INF, INF,  0 , INF, INF, INF},
                {INF,  1 , INF, INF, 2,  0 , INF, INF},
                {INF, INF, INF, INF, INF, INF,  0, INF },
                {INF, INF, INF, INF, INF, INF,  INF, 0 },
        };
        Matrix<Number> expectedDistMatrix = new Matrix<>(8, expectedDist);
        assertEquals(expectedDistMatrix, distResult, "The distance matrix is correct after 2 steps");
        assertEquals(expectedPredMatrix, predResult, "The predecessor matrix is correct after 2 steps");
    }
}