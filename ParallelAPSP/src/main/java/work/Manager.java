package work;

import memoryModel.*;
import memoryModel.topology.Topology;
import org.junit.platform.commons.util.ExceptionUtils;
import util.Matrix;

import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    // size of input
    private final int n;
    // number of processing elements
    private final int p;

    private final MemoryController memoryController;
    private final Matrix<Worker> workers;

    private final Matrix<Thread> workerThreads;
    // only one of the workers should execute stopWorkers, so this lock is used for exclusion
    private final Lock lock = new ReentrantLock();
    private boolean workersHaveBeenStopped = false;
    private boolean workHasBeenDone = false;

    // 1x1 version TODO: make general constructor

    /**
     * Creates a Manager. Upon construction, the manager will creates a MemoryController, and a matrix of n x n workers.
     * The workers will start their execution when {@link #doWork} is called, which will block until all workers have
     * finished. If any error occurs during execution, such as {@link CommunicationChannelCongestionException}
     * or {@link InconsistentMemoryChannelUsageException}, an exception will be thrown when {@link #getResult} is
     * called.
     *
     * @param n a matrix of n x n workers are created
     * @param numComputationPhases the number of computation phases each worker should perform
     * @param initialMemoryContent a map from private memory access labels to the content stored in each worker's memory
     * @param memoryTopology a constructor taking a non-negative integer and giving an object that subtypes Topology
     * @param workerFactory a WorkerFactory that has not yet been initialised.
     * @throws WorkerInstantiationException if any of the workers are not able to be constructed
     */
    public Manager(int n, int numComputationPhases, Map<String, Matrix<Number>> initialMemoryContent,
                   Function<Integer, ? extends Topology> memoryTopology, WorkerFactory workerFactory) throws WorkerInstantiationException {
        this.n = n;
        this.p = n;

        if (null != initialMemoryContent) {
            for (String s : initialMemoryContent.keySet()) {
                assert initialMemoryContent.get(s).size() == this.p;
            }
        }

        // initialize the private memory
        // used to fetch the results after computation
        Matrix<PrivateMemory> privateMemoryMatrix = new Matrix<>(this.p, () -> new PrivateMemory(1));
        if (null != initialMemoryContent) {
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    for (String s : initialMemoryContent.keySet()) {
                        privateMemoryMatrix.get(i, j).set(s, initialMemoryContent.get(s).get(i, j));
                    }
                }
            }
        }

        this.memoryController = new MemoryController(this.p, privateMemoryMatrix, memoryTopology);

        CyclicBarrier cyclicBarrier = new CyclicBarrier(this.p * this.p, () -> {
            try {
                this.memoryController.flush();
            } catch (InconsistentMemoryChannelUsageException e) {
                LOGGER.log(Level.WARNING, ExceptionUtils.readStackTrace(e));
                // interrupt all workers, including the thread that runs this, if flushing fails
                stopWorkers();
            }
        });

        // initialize the worker factory
        Runnable r = this::stopWorkers;
        workerFactory.init(memoryController, cyclicBarrier, r);

        // and create all the workers
        this.workers = new Matrix<>(this.p);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Worker w = workerFactory.createWorker(i, j, this.p, numComputationPhases, privateMemoryMatrix.get(i, j));
                this.workers.set(i, j, w);
            }
        }

        this.workerThreads = new Matrix<>(this.p);
    }

    /**
     * Starts all and blocks on the workers to finish in 3 phases:
     * * All the worker threads are created
     * * All the worker threads are started
     * * All the worker threads are joined
     *
     * @throws InterruptedException In case any worker thread gets interrupted while we are waiting for
     * them to be joined
     */
    public void doWork() throws InterruptedException {
        if (this.workHasBeenDone || this.workersHaveBeenStopped) {
            throw new IllegalStateException("Work cannot be performed twice or started when execution has previously failed");
        }
        // create all the threads
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Thread t = new Thread(this.workers.get(i, j));
                this.workerThreads.set(i, j, t);
            }
        }
        // we create all the threads before starting any of them. Otherwise, we may get null pointer exceptions if any
        // of them tries to interrupt another one in stopWorkers.
        synchronized (this.lock) {
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    Thread t = this.workerThreads.get(i, j);
                    LOGGER.log(Level.FINER, "Worker({0}, {1}) is being started with Thread ID={2}", new Object[]{i, j, t.getId()});
                    // TODO: set daemon?
                    t.start();
                }
            }
        }
        // then wait for them to finish
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                this.workerThreads.get(i, j).join();
                LOGGER.log(Level.FINER, "Worker({0}, {1}) has completed its work", new Object[]{i, j});
            }
        }

        this.workHasBeenDone = true;
    }

    /**
     * Gracefully interrupt all of the workers such that they will stop execution after finishing their current
     * computation or communication phase. This method will be run by one thread at a time, and when the functionality
     * has been performed, other threads running this method will not re-perform it.
     */
    private void stopWorkers() {
        synchronized (this.lock) {
            if (this.workersHaveBeenStopped) {
                return;
            }
            LOGGER.log(Level.FINE, "Thread-{0} is attempting to stop all workers.", Thread.currentThread().getId());
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    this.workerThreads.get(i, j).interrupt();
                }
            }
            this.workersHaveBeenStopped = true;
        }
    }

    /**
     * Returns the result of computation. A String label matching the memory location the workers store their result
     * in should be provided. This method creates a matrix of the Numbers found within each Worker's private memory
     * when accessed with the label {@code label}.
     *
     * @param label the string label. May be null, in which case an empty matrix is returned.
     * @return
     * @throws WorkersFailedToCompleteException
     */
    public Matrix<Number> getResult(String label) throws WorkersFailedToCompleteException {
        if (!this.workHasBeenDone) {
            throw new WorkersFailedToCompleteException("The workers were not started, so result cannot be fetched");
        } else if (this.workersHaveBeenStopped) {
            throw new WorkersFailedToCompleteException("An error was encountered during execution of workers");
        } else if (null == label) {
            return new Matrix<>(this.p);
        } else {
            // TODO: generalize
            Matrix<Number> resultMatrix = new Matrix<>(this.p);
            assert this.p == this.n;
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    resultMatrix.set(i, j, this.workers.get(i,j).read(label));
                }
            }
            return resultMatrix;
        }
    }
}
