package work;

import jdk.jfr.Description;
import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import memoryModel.topology.SquareGridTopology;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.max;
import static org.junit.jupiter.api.Assertions.*;

class WorkerManagerExceptionTests {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Test
    void workersCompleteWhenNoWorkToBeDone() {
        // SETUP
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);
        Class<? extends Worker> workerClass = EmptyWorker.class;

        Manager m;
        try {
            WorkerFactory wf = new WorkerFactory(workerClass);
            m = new Manager(3, 5, null, SquareGridTopology::new, wf);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Failed to construct manager.");
            return;
        }

        // ACT
        try {
            m.doWork();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Work failed to complete.");
        }

        // ASSERT
        try {
            m.getResult(null);
        } catch (WorkersFailedToCompleteException e) {
            e.printStackTrace();
            fail("Workers were  not able to complete execution");
        }
    }

    @Test
    @Description("Starts up 9 different Worker threads, which each wait a different amount of time, and " +
            " checks if all the thread exit within 5 second if one of the thread encounters a communication error.")
    void workersFailGracefullyOnCommunicationException() {
        // SETUP
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);
        Class<? extends Worker> workerClass = FailingWorker.class;

        Manager m;
        try {
            WorkerFactory wf = new WorkerFactory(workerClass);
            m = new Manager(3, 20, null, SquareGridTopology::new, wf);
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
                Thread.sleep(5000);
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
            workIsFinished.set(true);
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ASSERT
        assertTrue(workFinishedOnTime.get(), "The manager completed on time");
    }

    @Test
    void workersExitGracefullyOnInconsistentMemoryChannelUsage() {
        // SETUP
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);
        Class<? extends Worker> workerClass = InconsistentWorker.class;

        Manager m;
        try {
            WorkerFactory wf = new WorkerFactory(workerClass);
            m = new Manager(5, 20, null, SquareGridTopology::new, wf);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Failed to construct manager.");
            return;
        }

        // ACT
        try {
            m.doWork();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Manager was interrupted");
        }

        // ASSERT
        // the workers should be stopped before they reach phase 2
        assertTrue(InconsistentWorker.highestPhase <= 1);
    }
}

class EmptyWorker extends Worker {

    public EmptyWorker(int i, int j, int n, int numPhases, PrivateMemory privateMemory,
                       MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        super(i, j, n, numPhases, privateMemory, memoryController, cyclicBarrier, runExceptionHandler);
    }

    @Override
    void computation(int l) { }

    @Override
    void communicationBefore(int l) throws CommunicationChannelCongestionException { }

    @Override
    void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}

class FailingWorker extends Worker {
    public static int highestPhase = 0;

    public FailingWorker(int i, int j, int p, int numPhases, PrivateMemory privateMemory,
                         MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        super(i, j, p, numPhases, privateMemory, memoryController, cyclicBarrier, runExceptionHandler);
    }

    @Override
    void computation(int l) {
        try {
            Thread.sleep(2000 * this.i + 1000 * this.j);
        } catch (InterruptedException e) {
            // reset the interrupt flag because it was cleared
            Thread.currentThread().interrupt();
        }
    }

    @Override
    void communicationBefore(int l) throws CommunicationChannelCongestionException {
        synchronized (this) {
            highestPhase = max(highestPhase, l);
        }
    }

    @Override
    void communicationAfter(int l) throws CommunicationChannelCongestionException {
        if (i == 1 && j == 1) {
            throw new CommunicationChannelCongestionException("Communication failure");
        }
    }
}

class InconsistentWorker extends Worker {
    public static int highestPhase = 0;

    public InconsistentWorker(int i, int j, int p, int numPhases, PrivateMemory privateMemory,
                              MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        super(i, j, p, numPhases, privateMemory, memoryController, cyclicBarrier, runExceptionHandler);
    }

    @Override
    void computation(int l) {
        this.receive("A");
    }

    @Override
    void communicationBefore(int l) throws CommunicationChannelCongestionException {
        synchronized (this) {
            highestPhase = max(highestPhase, l);
        }
    }

    @Override
    void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}
