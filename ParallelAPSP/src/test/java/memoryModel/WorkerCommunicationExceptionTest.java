package memoryModel;

import org.junit.jupiter.api.Test;
import util.Matrix;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

class WorkerCommunicationExceptionTest {

    class EmptyWorker<T> extends memoryModel.Worker<T> {

        public EmptyWorker(int i, int j, int n, int numPhases, PrivateMemory<T> privateMemory, MemoryController<T> memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
            super(i, j, n, numPhases, privateMemory, memoryController, cyclicBarrier, runExceptionHandler);
        }

        @Override
        void computation(int l) { }

        @Override
        void communicationBefore(int l) throws CommunicationChannelCongestionException { }

        @Override
        void communicationAfter(int l) throws CommunicationChannelCongestionException { }

        // TODO: create new method that needs to be overriden, a static Worker supplier that
        //       would return EmptyWorkers, and that method is used by the manager. This avoids
        //       all the reflection stuff. Also write a note about what I've tried so far....
    }

    @Test
    void workersCompleteWhenNoWorkToBeDone() {
        Map<String, Matrix<Integer>> initialMemory = new HashMap<>();
//        try {
//            Class<? extends Worker<Integer>> workerClass = EmptyWorker<Integer>
//            Manager<Integer> m = new Manager<Integer>(3, initialMemory, EmptyWorker.class);
//        } catch (WorkerInstantiationException e) {
//            e.printStackTrace();
//            fail("The workers should be instantiated successfully");
//        }
    }
}