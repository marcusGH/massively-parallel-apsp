package memoryModel;

import com.sun.jdi.IntegerType;
import util.Matrix;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    // size of input
    private final int n;
    // number of processing elements
    private final int p;
    // number of phases of computation in algorithm
    private final int numComputationPhases;

    private final CyclicBarrier cyclicBarrier;
    private final MemoryController memoryController;
    // used to fetch the results after computation
    private final Matrix<PrivateMemory> privateMemoryMatrix;
    private final Matrix<Worker> workers;

    private final Matrix<Thread> workerThreads;
    // to synchronize worker stopping
    private final Lock lock = new ReentrantLock();

    // 1x1 version TODO: make general constructor
    public Manager(int n, int numComputationPhases, Map<String, Matrix<Number>> initialMemoryContent,
                   Function<Integer, ? extends Topology> memoryTopology, WorkerFactory workerFactory) throws WorkerInstantiationException {
        this.n = n;
        this.p = n;
        this.numComputationPhases = numComputationPhases;

        if (null != initialMemoryContent) {
            for (String s : initialMemoryContent.keySet()) {
                assert initialMemoryContent.get(s).size() == this.p;
            }
        }

        // initialize the private memory
        this.privateMemoryMatrix = new Matrix<>(this.p, () -> new PrivateMemory(1));
        if (null != initialMemoryContent) {
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    for (String s : initialMemoryContent.keySet()) {
                        this.privateMemoryMatrix.get(i, j).set(s, initialMemoryContent.get(s).get(i, j));
                    }
                }
            }
        }

        this.memoryController = new MemoryController(this.p, this.privateMemoryMatrix, memoryTopology);

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

        this.workers = new Matrix<>(this.p);
        // pass the same runnable to all workers such that we can use it as a lock
        Runnable r = this::stopWorkers;
        workerFactory.init(memoryController, cyclicBarrier, r);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Worker w = workerFactory.createWorker(i, j, this.p, numComputationPhases, this.privateMemoryMatrix.get(i, j));
                this.workers.set(i, j, w);
//                Worker w = workerFactory.cre
//                this.workers()
//                try {
//                    // TODO: work from here jk jk: Problem right now is that the constructor is not found when trying this
//                    //       addtionally, this is some nasty code, so do some major refactoring by introducting the factory
//                    //       pattern that constructs the Workers, and we subclass the factory for each implementation of Worker
//
//                    Class[] workerConstructorParameterTypes = {
//                            Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, PrivateMemory.class,
//                            MemoryController.class, CyclicBarrier.class, Runnable.class
//                    };
//                    Constructor<?>[] constructors = workerClass.getConstructors();
//                    System.out.println(constructors[0]);
//                    Constructor<? extends Worker> workerConstructor = workerClass.getConstructor(workerConstructorParameterTypes);
//                    Worker w = workerConstructor.newInstance(i, j, this.p, this.numComputationPhases,
//                            this.privateMemoryMatrix.get(i, j), this.memoryController, this.cyclicBarrier, r);
//                    this.workers.set(i, j, w);
//                } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
//                    e.printStackTrace();
//                    throw new WorkerInstantiationException(String.format("Worker(%d, %d) could not be instantiated.", i, j));
//                }
            }
        }

        this.workerThreads = new Matrix<>(this.p);
    }

    public void doWork() throws InterruptedException {
        // start all the threads
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Thread t = new Thread(this.workers.get(i, j));
                LOGGER.log(Level.FINER, "Worker({0}, {1}) is being started with Thread ID={2}", new Object[]{i, j, t.getId()});
                t.start();
                // TODO: set daemon?
                this.workerThreads.set(i, j, t);
            }
        }
        // then wait for them to finish
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                this.workerThreads.get(i, j).join();
                LOGGER.log(Level.FINER, "Worker({0}, {1}) has completed its work", new Object[]{i, j});
            }
        }
    }

    private void stopWorkers() {
        synchronized (this.lock) {
            LOGGER.log(Level.FINE, "Thread-{0} is attempting to stop all workers.", Thread.currentThread().getId());
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    this.workerThreads.get(i, j).interrupt();
                }
            }
        }
    }
}
