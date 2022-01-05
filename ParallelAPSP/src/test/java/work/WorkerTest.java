package work;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WorkerTest {

    @Test
    void workerCompletesWithoutFailure() {
        // SETUP
        final AtomicInteger barrierCount = new AtomicInteger(0);
        final AtomicBoolean computationFailed = new AtomicBoolean(false);
        final AtomicInteger comulativeSum = new AtomicInteger(0);

        CyclicBarrier cb = new CyclicBarrier(1, barrierCount::incrementAndGet);
        Runnable runExceptionHandler = () -> { computationFailed.set(true); };
        Worker w = new ComputationOnlyWorker(0, 0, 1, 10, new PrivateMemory(1),
                null, cb, runExceptionHandler, () -> {
            int a = 5;
            int b = a * 3;
            comulativeSum.addAndGet(b);
        });

        // ACT
        w.run();

        // ASSERT
        assertEquals(barrierCount.get(), 20, "The worker synchronised 20 times when doing 10 phases.");
        assertEquals(comulativeSum.get(), 15 * 10, "The worker did 10 computation phases and produced correct value");
        assertFalse(computationFailed.get(), "The worker completed computation without exception");
    }

    @Test
    void workerCanReadPrivateMemory() {
        // SETUP
        final AtomicBoolean computationFailed = new AtomicBoolean(false);
        final PrivateMemory pm = new PrivateMemory(6);
        pm.set(0, 0, "A", 0.01);
        pm.set(0, 1, "A", 0.1);
        pm.set(0, 2, "A", 0.2);
        pm.set(0, 3, "A", 0.3);
        pm.set(0, 4, "A", 0.4);
        pm.set(0, 5, "A", 0.5); // sum is 1.51

        CyclicBarrier cb = new CyclicBarrier(1);
        Runnable runExceptionHandler = () -> { computationFailed.set(true); };

        Worker w = new SimpleComputationWorker(0, 0, 1, 6, pm,
                null, cb, runExceptionHandler);

        // ACT
        w.run();
        double result = w.getPrivateMemory().getDouble("C");

        // ASSERT
        assertFalse(computationFailed.get(), "The worker completed computation without exception");
        assertEquals(result, 1.51, "The worker produced the correct sum");
    }
}

class ComputationOnlyWorker extends Worker {
    private final Runnable computation;

    protected ComputationOnlyWorker(int i, int j, int p, int numPhases, PrivateMemory privateMemory, MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler, Runnable computation) {
        super(i, j, p, numPhases, privateMemory, memoryController, cyclicBarrier, runExceptionHandler);
        this.computation = computation;
    }

    @Override
    void computation(int l) {
        this.computation.run();
    }

    @Override
    void communicationBefore(int l) throws CommunicationChannelCongestionException { }

    @Override
    void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}

class SimpleComputationWorker extends Worker {

    protected SimpleComputationWorker(int i, int j, int p, int numPhases, PrivateMemory privateMemory,
                                      MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        super(i, j, p, numPhases, privateMemory, memoryController, cyclicBarrier, runExceptionHandler);
    }

    /**
     * Stores in C the sum of all the values in private memory location (0, l) for all l
     * @param l a non-negative integer representing number of computation phases already completed
     */
    @Override
    void computation(int l) {
        if (l == 0) {
            // try shorthand reading
            store("C", read("A"));
        } else {
            store("C", read(0, l, "A") + read("C"));
        }
    }

    @Override
    void communicationBefore(int l) throws CommunicationChannelCongestionException {

    }

    @Override
    void communicationAfter(int l) throws CommunicationChannelCongestionException {

    }
}