package work;

import memoryModel.*;
import memoryModel.topology.Topology;
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
    // to synchronize worker stopping
    private final Lock lock = new ReentrantLock();
    private boolean workersHaveBeenStopped = false;
    private boolean workHasBeenDone = false;

    // 1x1 version TODO: make general constructor
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

        // TODO: not make private field, but rather localize to constructor?
        // TODO: if this doesn't work, the method will have to be static
        CyclicBarrier cyclicBarrier = new CyclicBarrier(this.p * this.p, () -> {
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
                Worker w = workerFactory.createWorker(i, j, this.p, numComputationPhases, privateMemoryMatrix.get(i, j));
                this.workers.set(i, j, w);
            }
        }

        this.workerThreads = new Matrix<>(this.p);
    }

    public void doWork() throws InterruptedException {
        // create all the threads
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Thread t = new Thread(this.workers.get(i, j));
                this.workerThreads.set(i, j, t);
            }
        }
        // we start them separately to avoid null pointer exception when one thread tries to interrupt the others
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
