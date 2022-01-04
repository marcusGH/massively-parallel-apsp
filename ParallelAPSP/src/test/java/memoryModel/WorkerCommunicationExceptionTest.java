package memoryModel;

import org.junit.jupiter.api.Test;
import util.LoggerFormatter;
import util.Matrix;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

class WorkerCommunicationExceptionTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Test
    void workersCompleteWhenNoWorkToBeDone() {
        LoggerFormatter.setupLogger(LOGGER, Level.ALL);

        Map<String, Matrix<Integer>> initialMemory = new HashMap<>();
        Class<? extends Worker> workerClass = EmptyWorker.class;

        Manager m;
        try {
            WorkerFactory wf = new WorkerFactory(workerClass);
            m = new Manager(3, 3, null, SquareGridTopology::new, wf);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            fail("Failed to construct manager.");
            return;
        }

        try {
            m.doWork();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Work failed to complete.");
        }
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
