package work;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.CommunicationChannelException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;
import util.Matrix;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ManagerTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);
    }

    @Test
    void simpleComputationWithCommunicationGivesCorrectValue() {
        // SETUP

        // create the initial memory to be:
        // 1 2 3 4
        // 2 4 6 8
        // 1 1 1 1
        // 5 10 15 20
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        Matrix<Number> matrix = new Matrix<>(4);
        matrix.setRow(0, Arrays.asList(1.0, 2.0, 3.0, 4.0));
        matrix.setRow(1, Arrays.asList(2.0, 4.0, 6.0, 8.0));
        matrix.setRow(2, Arrays.asList(1.0, 1.0, 1.0, 1.0));
        matrix.setRow(3, Arrays.asList(5.0, 10.0, 15.0, 20.0));
        initialMemory.put("A", matrix);

        // create the manager
        Manager m;
        try {
            m = new Manager(4, 4, initialMemory, SimpleCommunicatingWorker.class);
        } catch (WorkerInstantiationException e) {
            fail("The manager could not create all the workers");
            return;
        }

        // ACT
        Matrix<Number> result;
        try {
            m.doWork();
            result = m.getResult("C");
        } catch (CommunicationChannelException e) {
            fail("Manager failed to complete work due to interruption");
            return;
        } catch (WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("The workers encountered an error during execution");
            return;
        }

        // ASSERT

        // expected result
        Matrix<Double> expected = new Matrix<>(4);
        expected.setRow(0, Collections.nCopies(4, 10.0));
        expected.setRow(1, Collections.nCopies(4, 20.0));
        expected.setRow(2, Collections.nCopies(4, 4.0));
        expected.setRow(3, Collections.nCopies(4, 50.0));

        assertEquals(expected, result, "The result is as expected");
    }

    @Test
    void noRaceConditionsInSimpleWorker() {
        // SETUP

        // create the initial memory to be:
        // 1 2 3 4
        // 2 4 6 8
        // 1 1 1 1
        // 5 10 15 20
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        Matrix<Number> matrix = new Matrix<>(4);
        matrix.setRow(0, Arrays.asList(1.0, 2.0, 3.0, 4.0));
        matrix.setRow(1, Arrays.asList(2.0, 4.0, 6.0, 8.0));
        matrix.setRow(2, Arrays.asList(1.0, 1.0, 1.0, 1.0));
        matrix.setRow(3, Arrays.asList(5.0, 10.0, 15.0, 20.0));
        initialMemory.put("A", matrix);

        // create the manager
        Manager m;
        try {
            m = new Manager(4, 4, initialMemory, SimpleCommunicatingWorker.class);
        } catch (WorkerInstantiationException e) {
            fail("The manager could not create all the workers");
            return;
        }

        // ACT and ASSERT many times

        // expected result
        Matrix<Double> expected = new Matrix<>(4);
        expected.setRow(0, Collections.nCopies(4, 10.0));
        expected.setRow(1, Collections.nCopies(4, 20.0));
        expected.setRow(2, Collections.nCopies(4, 4.0));
        expected.setRow(3, Collections.nCopies(4, 50.0));

        for (int i = 0; i < 50; i++) {
            // reset the memory first
            if (i != 0) {
                m.setPrivateMemory(initialMemory);
            }
            // then compute
            Matrix<Number> result;
            try {
                m.doWork();
                result = m.getResult("C");
            } catch (CommunicationChannelException e) {
                fail("Manager failed to complete work due to interruption");
                return;
            } catch (WorkersFailedToCompleteException e) {
                fail("The workers encountered an error during execution");
                return;
            }

            assertEquals(expected, result, "The results are as expected on iteration " + i);
        }
    }

    @Test
    void broadcastingWorkerProducesCorrectResult() {
        // SETUP

        // create the initial memory in "A" to be:
        // 1 2 3
        // 4 5 6
        // 7 8 9
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        Matrix<Number> matrix = new Matrix<>(3);
        matrix.setRow(0, Arrays.asList(1.0, 2.0, 3.0));
        matrix.setRow(1, Arrays.asList(4.0, 5.0, 6.0));
        matrix.setRow(2, Arrays.asList(7.0, 8.0, 9.0));
        initialMemory.put("A", matrix);

        // create the manager
        Manager m;
        try {
            m = new Manager(3, 3, initialMemory, BroadcastingWorker.class);
        } catch (WorkerInstantiationException e) {
            fail("The manager could not create all the workers");
            return;
        }

        // ACT
        Matrix<Number> result;
        try {
            m.doWork();
            result = m.getResult("C");
        } catch (WorkersFailedToCompleteException | CommunicationChannelException e) {
            fail("The workers encountered an error during execution");
            return;
        }

        // ASSERT

        // The algorithm is just matrix multiplication, so we take the square of the matrix
        // 1 2 3     30  36  42
        // 4 5 6  -> 66  81  96
        // 7 8 9     102 126 150
        // expected result
        Matrix<Double> expected = new Matrix<>(3);
        expected.setRow(0, Arrays.asList(30.0, 36.0, 42.0));
        expected.setRow(1, Arrays.asList(66.0, 81.0, 96.0));
        expected.setRow(2, Arrays.asList(102.0, 126.0, 150.0));

        assertEquals(expected, result, "The result is as expected");
    }

    // TODO: another test where the same PE receives data from **different** nodes at each phase

    @Test
    void managerCanCreateALotOfWorkers() {
        // SETUP
        final int n = 50;
        Manager m;
        try {
            m = new Manager(n, n, null, EmptyWorker.class);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("The manager could not be created");
            return;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            fail("The manager could not create the specified number of threads");
            return;
        }

        // ACT
        try {
            LoggerFormatter.setupLogger(LOGGER, Level.INFO);
            m.doWork();
            m.getResult(null);
            LoggerFormatter.setupLogger(LOGGER, Level.ALL);
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("The workers did not complete computation successfully");
        }

        // ASSERT
        assertTrue(true, "The workers completed their empty tasks successfully");
    }
}

/**
 * All the workers pass their own held value to the right neighbour after each computation, then
 * they compute the cumulative sums of whatever value they're given.
 */
class SimpleCommunicatingWorker extends Worker {

    public SimpleCommunicatingWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) {
        if (l == 0) {
            store("C", readDouble("A"));
        } else {
            store("C", readDouble("A") + readDouble("C"));
        }
        System.out.println(String.format("W(%d, %d) reads A=%f", i, j, readDouble("A")));
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException { }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        // send to right, wrapping around if necessary
        send(i, (j + 1) % p, readDouble("A"));
        // then receive data from the left
        receive("A");
    }
}

/**
 * At each computation phase, the worker will add the product of whatever it gets from the row-
 * and column-highways to a cumulative sum, stored in "C". At each communication phase l, the rows highways
 * will be used by (*, l) and the column highways by (l, *)
 */
class BroadcastingWorker extends Worker {

    public BroadcastingWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) {
        if (l == 0) {
            store("C", 0);
        }
        double value = readDouble("rowA") * readDouble("colA") + readDouble("C");
        store("C", value);
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        if (i == l) {
            broadcastCol(readDouble("A"));
        }
        if (j == l) {
            broadcastRow(readDouble("A"));
        }
        receiveRowBroadcast("rowA");
        receiveColBroadcast("colA");
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}
