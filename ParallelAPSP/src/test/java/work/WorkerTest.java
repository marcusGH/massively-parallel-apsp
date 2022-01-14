package work;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WorkerTest {

    @Test
    void workerCompletesWithoutFailure() {
        // SETUP
        final AtomicInteger comulativeSum = new AtomicInteger(0);

        Worker w = new ComputationOnlyWorker(0, 0, 1, 1, 10, new PrivateMemory(1),
                null, () -> {
            int a = 5;
            int b = a * 3;
            comulativeSum.addAndGet(b);
        });

        // ACT
        w.run();

        // ASSERT
        assertEquals(comulativeSum.get(), 15 * 10, "The worker did 10 computation phases and produced correct value");
    }

    @Test
    void workerCanReadPrivateMemory() {
        // SETUP
        final PrivateMemory pm = new PrivateMemory(6);
        pm.set(0, 0, "A", 0.01);
        pm.set(0, 1, "A", 0.1);
        pm.set(0, 2, "A", 0.2);
        pm.set(0, 3, "A", 0.3);
        pm.set(0, 4, "A", 0.4);
        pm.set(0, 5, "A", 0.5); // sum is 1.51

        Worker w = new SimpleComputationWorker(0, 0, 1, 1, 6, pm,
                null);

        // ACT
        w.run();
        double result = w.getPrivateMemory().getDouble("C");

        // ASSERT
        assertEquals(result, 1.51, "The worker produced the correct sum");
    }
}

class ComputationOnlyWorker extends Worker {
    private final Runnable computation;

    public ComputationOnlyWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController, Runnable computation) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
        this.computation = computation;
    }

    @Override
    protected void initialise() { }

    @Override
    public void computation(int l) {
        this.computation.run();
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException { }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}

class SimpleComputationWorker extends Worker {

    public SimpleComputationWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }

    @Override
    protected void initialise() { }

    /**
     * Stores in C the sum of all the values in private memory location (0, l) for all l
     * @param l a non-negative integer representing number of computation phases already completed
     */
    @Override
    public void computation(int l) {
        if (l == 0) {
            // try shorthand reading
            store("C", readDouble("A"));
        } else {
            store("C", readDouble(0, l, "A") + readDouble("C"));
        }
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException { }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException { }
}