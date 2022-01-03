package memoryModel;

import util.Matrix;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class Manager<T> {
    // size of input
    private final int n;
    // number of processing elements
    private final int p;
    // number of phases of computation in algorithm
    private final int numComputationPhases;

    private final CyclicBarrier cyclicBarrier;
    private final MemoryController<T> memoryController;
    // used to fetch the results after computation
    private final Matrix<PrivateMemory<T>> privateMemoryMatrix;
    private final Matrix<Worker<T>> workers;

    private Matrix<Thread> workerThreads;
    // to synchronize worker stopping
    private final Lock lock = new ReentrantLock();

    // 1x1 version TODO: make general constructor
    public Manager(int n, int numComputationPhases, Map<String, Matrix<T>> initialMemoryContent,
                   Function<Integer, ? extends Topology> memoryTopology,
                   Constructor<? extends Worker<T>> workerConstructor) throws WorkerInstantiationException {
        this.n = n;
        this.p = n;
        this.numComputationPhases = numComputationPhases;

        if (null != initialMemoryContent) {
            for (String s : initialMemoryContent.keySet()) {
                assert initialMemoryContent.get(s).size() == this.p;
            }
        }

        // initialize the private memory
        this.privateMemoryMatrix = new Matrix<>(this.p, () -> new PrivateMemory<>(1));
        if (null != initialMemoryContent) {
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    for (String s : initialMemoryContent.keySet()) {
                        this.privateMemoryMatrix.get(i, j).set(s, initialMemoryContent.get(s).get(i, j));
                    }
                }
            }
        }

        this.memoryController = new MemoryController<T>(this.p, this.privateMemoryMatrix, memoryTopology);

        // TODO: not make private field, but rather localize to constructor?
        this.cyclicBarrier = new CyclicBarrier(this.p * this.p, () -> {
            try {
                this.memoryController.flush();
            } catch (InconsistentMemoryChannelUsageException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                // TODO: if this doesn't work, the method will have to be static
                stopWorkers();
            }
        });

        // TODO: work from here jk. Take a step back and rethink my approach on Manager genericism
        // TODO: a factory approach would be better. We shouldn't need all this reflection to handle this,
        // TODO: but it would be better to have a simple solution, refactoring on larger scale, so take a step back

        this.workers = new Matrix<>(this.p);
        // pass the same runnable to all workers such that we can use it as a lock
        Runnable r = this::stopWorkers;
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                try {
                    Worker<T> w = workerConstructor.newInstance(i, j, this.p, this.numComputationPhases,
                            this.privateMemoryMatrix.get(i, j), this.memoryController, this.cyclicBarrier, r);
                    this.workers.set(i, j, w);
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new WorkerInstantiationException(String.format("Worker(%d, %d) could not be instantiated.", i, j));
                }
            }
        }
    }

    public void doWork() throws InterruptedException {
        // start all the threads
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Thread t = new Thread(this.workers.get(i, j));
                t.start();
                // TODO: set daemon?
                this.workerThreads.set(i, j, t);
            }
        }
        // then wait for them to finish
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                this.workerThreads.get(i, j).join();
                System.out.println(String.format("Worker(%d, %d) has completed its work", i, j));
            }
        }
    }

    private void stopWorkers() {
        synchronized (this.lock) {
            System.out.println(String.format("Thread-%d is stopping the workers.", Thread.currentThread().getId()));
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    this.workerThreads.get(i, j).interrupt();
                }
            }
        }
    }
}
