package work;

import jdk.jfr.Description;
import memoryModel.CommunicationChannelCongestionException;
import memoryModel.CommunicationChannelException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import memoryModel.topology.SquareGridTopology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.max;
import static org.junit.jupiter.api.Assertions.*;

class WorkerManagerExceptionTests {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);
    }

    @Test
    void workersCompleteWhenNoWorkToBeDone() {
        // SETUP
        Class<? extends Worker> workerClass = EmptyWorker.class;

        Manager m;
        try {
            m = new Manager(3, 5, null, SquareGridTopology::new, EmptyWorker.class);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Failed to construct manager.");
            return;
        }

        // ACT
        try {
            m.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("Work failed to complete.");
        }

        // ASSERT
        m.getResult(null);
        assertTrue(true, "Workers completed successfully");
    }

    @Test
    @Description("Starts up 9 different Worker threads, which each wait a different amount of time, and " +
            " checks if all the thread exit within 5 second if one of the thread encounters a communication error.")
    void workersFailGracefullyOnCommunicationException() {
        // SETUP
        Class<? extends Worker> workerClass = FailingWorker.class;

        Manager m;
        try {
            m = new Manager(3, 20, null, SquareGridTopology::new, workerClass);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Failed to construct manager.");
            return;
        }
        final AtomicBoolean workIsFinished = new AtomicBoolean(false);
        final AtomicBoolean workFinishedOnTime = new AtomicBoolean(false);

        // asynchronously fail if the work takes more than 5 seconds because we should shut down gracefully after 3
        // seconds
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) { }
            if (!workIsFinished.get()) {
                workFinishedOnTime.set(false);
            } else {
                workFinishedOnTime.set(true);
            }
        });

        // ACT
        try {
            t.start();
            m.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
        }

        workIsFinished.set(true);

        try {
            t.join();
        } catch (InterruptedException ignored) {
            fail("Thread was unexpectedly interrupted");
        }

        // ASSERT
        assertTrue(workFinishedOnTime.get(), "The manager completed on time");
    }

    @Test
    void workersExitGracefullyOnInconsistentMemoryChannelUsage() {
        // SETUP
        Class<? extends Worker> workerClass = InconsistentWorker.class;

        Manager m;
        try {
            m = new Manager(5, 20, null, SquareGridTopology::new, workerClass);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Failed to construct manager.");
            return;
        }

        // ACT
        try {
            m.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
        }

        // ASSERT
        // the workers should be stopped before they reach phase 2
        assertTrue(InconsistentWorker.highestPhase <= 1);
    }
}

/**
 * This worker does nothing, but is just used for stress testing
 */
class EmptyWorker extends Worker {

    public EmptyWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) { }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException { }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}

class FailingWorker extends Worker {
    public static int highestPhase = 0;

    public FailingWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) {
        try {
            Thread.sleep(2000 * this.i + 1000 * this.j);
        } catch (InterruptedException e) {
            // reset the interrupt flag because it was cleared
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        synchronized (this) {
            highestPhase = max(highestPhase, l);
        }
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        if (i == 1 && j == 1) {
            throw new CommunicationChannelCongestionException("Communication failure");
        }
    }
}

class InconsistentWorker extends Worker {
    public static int highestPhase = 0;

    public InconsistentWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    public void initialise() { }

    @Override
    public void computation(int l) {
        this.receive("A");
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        synchronized (this) {
            highestPhase = max(highestPhase, l);
        }
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}
