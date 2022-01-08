package work;

import memoryModel.*;
import memoryModel.topology.Topology;
import org.junit.platform.commons.util.ExceptionUtils;
import util.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final int MAX_CONCURRENT_THREADS = 16;

    // size of input
    private final int n;
    // number of processing elements
    private final int p;

    private final int numComputationPhases;

    private final MemoryController memoryController;
    private final Matrix<PrivateMemory> privateMemoryMatrix;
    private final Matrix<Worker> workers;

    private final ExecutorService executorService;
    // only one of the workers should execute stopWorkers, so this lock is used for exclusion
    private boolean workHasBeenDone = false;

    // 1x1 version TODO: make general constructor

    /**
     * Creates a Manager. Upon construction, the manager will creates a MemoryController, and a matrix of n x n workers.
     * The workers will start their execution when {@link #doWork} is called, which will block until all workers have
     * finished. If any error occurs during execution, such as {@link CommunicationChannelCongestionException}
     * or {@link InconsistentCommunicationChannelUsageException}, an exception will be thrown when {@link #getResult} is
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
        this.numComputationPhases = numComputationPhases;

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

        this.privateMemoryMatrix = privateMemoryMatrix;
        this.memoryController = new MemoryController(this.p, privateMemoryMatrix, memoryTopology);

        workerFactory.init(memoryController);

        // and create all the workers
        this.workers = new Matrix<>(this.p);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Worker w = workerFactory.createWorker(i, j, this.p, this.n, numComputationPhases, privateMemoryMatrix.get(i, j));
                this.workers.set(i, j, w);
            }
        }

        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);
    }

    public void resetMemory(Map<String, Matrix<Number>> memoryContent) {
        assert memoryContent != null;
        // TODO: modify when generalizing everything
        for (String s : memoryContent.keySet()) {
            assert memoryContent.get(s).size() == this.p;
        }

        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                for (String s : memoryContent.keySet()) {
                    this.privateMemoryMatrix.get(i, j).set(s, memoryContent.get(s).get(i, j));
                }
            }
        }
    }

    /**
     * We want to return a list of features so that we can look at exceptions thrown during execution by workers
     * and report them back to the programmer.
     *
     * @param phaseNumber  non-negative integer id
     * @param phaseType a enum of the possible worker phases
     * @return a list of futures produced by the ExecutorService
     */
    private List<Future<?>> startWorkerExecution(int phaseNumber, Worker.WorkerPhases phaseType) {
        List<Future<?>> workerFutures = new ArrayList<>(this.p * this.p);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Callable<Object> workerTask;
                switch (phaseType) {
                    case COMMUNICATION_BEFORE:
                        workerTask = this.workers.get(i, j).getCommunicationBeforeCallable(phaseNumber);
                        break;
                    case COMPUTATION:
                        workerTask = this.workers.get(i, j).getComputationCallable(phaseNumber);
                        break;
                    case COMMUNICATION_AFTER:
                        workerTask = this.workers.get(i, j).getCommunicationAfterCallable(phaseNumber);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + phaseType);
                }
                LOGGER.log(Level.FINER, "Phase {0}: Worker({1}, {2}) is being started.", new Object[]{phaseNumber, i, j});
                Future<?> workerFuture = this.executorService.submit(workerTask);
                workerFutures.add(workerFuture);
            }
        }
        return workerFutures;
    }

    /**
     * Blocks until all workers in the workerFutures have completed execution. Throws an exception wrapping any
     * exception thrown by any of the workers in case they fail.
     *
     * @param workerFutures A list of worker futures returned by an ExecutorService
     * @throws WorkersFailedToCompleteException is thrown if any worker encountered a failure
     */
    private void checkForWorkerFailure(List<Future<?>> workerFutures) throws WorkersFailedToCompleteException, CommunicationChannelException {
        for (Future<?> f : workerFutures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                // don't do any more work, because one or more workers have failed
                this.executorService.shutdown();
                // unwrap communication channel exception from execution exception
                if (e.getCause() instanceof CommunicationChannelException) {
                    throw (CommunicationChannelException) e.getCause();
                }
                // otherwise just throw a more general exception, wrapping what was thrown
                throw new WorkersFailedToCompleteException("A worker failed during execution", e);
            }
        }
    }
    /**
     * Starts all and blocks on the workers to finish in 3 phases:
     * * All the worker threads are created
     * * All the worker threads are started
     * * All the worker threads are joined
     *
     * TODO: merge memory exceptions
     * TODO: redo docs
     *
     * @throws InterruptedException In case any worker thread gets interrupted while we are waiting for
     * them to be joined
     */
    public void doWork() throws CommunicationChannelException, WorkersFailedToCompleteException {
        // TODO: find better wya of handling repeated work
//        if (this.workHasBeenDone) {
//            throw new IllegalStateException("Work cannot be performed twice or started when execution has previously failed");
//        }

        LOGGER.log(Level.INFO, "Manager is starting {0} phases of work with {1} workers.", new Object[]{this.numComputationPhases, this.p * this.p});

        List<Future<?>> workerFutures;
        for (int l = 0; l < this.numComputationPhases; l++) {
            // COMMUNICATION_BEFORE phase
            LOGGER.log(Level.FINE, "Manager is starting communicationBefore phase {0}", l);
            workerFutures = startWorkerExecution(l, Worker.WorkerPhases.COMMUNICATION_BEFORE);
            checkForWorkerFailure(workerFutures);
            this.memoryController.flush();

            // COMPUTATION phase (no exception can be thrown here)
            LOGGER.log(Level.FINE, "Manager is starting computation phase {0}", l);
            startWorkerExecution(l, Worker.WorkerPhases.COMPUTATION);

            // COMMUNICATION_AFTER phase
            LOGGER.log(Level.FINE, "Manager is starting communicationAfter phase {0}", l);
            workerFutures = startWorkerExecution(l, Worker.WorkerPhases.COMMUNICATION_AFTER);
            checkForWorkerFailure(workerFutures);
            this.memoryController.flush();
        }

        LOGGER.log(Level.INFO, "Manager has completed {0} phases of work.", this.numComputationPhases);

        this.workHasBeenDone = true;
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
    public Matrix<Number> getResult(String label)  {
        if (!this.workHasBeenDone) {
            throw new IllegalStateException("The workers were not started, so result cannot be fetched");
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
