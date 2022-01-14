package matrixMultiplication;

import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;
import util.Matrix;
import work.Manager;
import work.WorkerFactory;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class BroadcastMatrixMultiplicationTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);
    }

    @Test
    void matrixMultiplicationProducesCorrectResult1() {
        // SETUP
        Double[][] A = {
                {1., 5., 42.},
                {6., 7., 12.},
                {45., 12., 45.}
        };
//        A = new Double[][]{
//                {1., 1., 0.},
//                {0., 1., 0.},
//                {0., 0., 1.}
//        };
        Double[][] B = {
                {0., -23., 2331.,},
                {3.14, 23.232, -1.},
                {2.71, 1., 1.}
        };
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        initialMemory.put("A", new Matrix<>(3, A));
        initialMemory.put("B", new Matrix<>(3, B));

        WorkerFactory wf;
        Manager m;
        try {
            m = new Manager(3, 3,  initialMemory, SquareGridTopology::new, BroadcastMatrixMultiplication.class);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Could not create worker or manager");
            return;
        }

        // ACT
        try {
            m.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("Could not execute workers successfully");
        }

        // ASSERT
        Double[][] C = {
                {129.52, 135.16, 2368.,},
                {54.5, 36.624, 13991.,},
                {159.63, -711.216, 104928.,}
        };
        Matrix<Number> Cmat = new Matrix<>(3, C);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(Cmat.get(i, j).doubleValue(), m.getResult("C").get(i, j).doubleValue(),
                        0.001, "The entry has the correct value");
            }
        }
    }

}