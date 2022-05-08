package work;

import memoryModel.*;
import util.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final int MAX_CONCURRENT_THREADS = 8;

    // number of rows and columns in input
    private final int n;
    // number of processing elements
    private final int p;
    private final int numComputationPhases;

    private final Class<? extends Worker> algorithm;

    private CommunicationManager communicationManager;
    private final Matrix<PrivateMemory> privateMemoryMatrix;
    private final Matrix<Worker> workers;

    private ExecutorService executorService;
    private boolean workHasBeenDone = false;

    /**
     * This constructed does not make a deep-copy of the passed manager, but re-uses all of its state. It is to be
     * used by children of Manager that wants to decorate the manager functionality someway.
     *
     * @param manager an instantiated manager
     */
    protected Manager(Manager manager) {
        // copy all the references
        this.n = manager.n;
        this.p = manager.p;
        this.numComputationPhases = manager.numComputationPhases;
        this.communicationManager = manager.communicationManager;
        this.privateMemoryMatrix = manager.privateMemoryMatrix;
        this.executorService = manager.executorService;
        this.workers = manager.workers;
        this.algorithm = manager.algorithm;
    }

    /**
     * Creates a Manager. Upon construction, the manager will creates a MemoryController, and a matrix of n x n workers.
     * The workers will start their execution when {@link #doWork} is called, which will block until all workers have
     * finished. If any error occurs during execution, such as {@link CommunicationChannelCongestionException}
     * or {@link InconsistentCommunicationChannelUsageException}, an exception will be thrown.
     *
     * @param n integer - the problem size
     * @param p a matrix of p x p workers are created
     * @param numComputationPhases the number of computation phases each worker should perform
     * @param initialMemoryContent a map from private memory access labels to the content stored in each worker's memory.
     *                             This parameter may be null, in which case all workers start with empty memory.
     * @param workerClass a subtype of Worker, specifying that computation and communication each individual worker should do
     * @throws WorkerInstantiationException if any of the workers are not able to be constructed
     */
    public Manager(int n, int p, int numComputationPhases, Map<String, Matrix<Number>> initialMemoryContent,
                   Class<? extends Worker> workerClass) throws WorkerInstantiationException {
        this.n = n;
        this.p = p;
        this.numComputationPhases = numComputationPhases;
        this.algorithm = workerClass;

        // not possible to divide up memory evenly
        if (n % p != 0) {
            throw new IllegalArgumentException(String.format("The passed initial memory does to distribute well among the PEs." +
                    " A matrix of size %d cannot be spread among %d PEs.", n, p));
        }

        // no memory provided
        if (null == initialMemoryContent) {
            // the private memory is used to fetch the results after computation, so save a reference to it
            this.privateMemoryMatrix =  new Matrix<>(p, () -> new PrivateMemory(n / p));
        } else {
            this.privateMemoryMatrix = new Matrix<>(this.p, () -> new PrivateMemory(n / p));
            this.setPrivateMemory(initialMemoryContent);
        }

        // TODO: refactor to make privateMemoryMatrix be local variable, and don't use as field
        //       also consider not referecing PrivateMemory at all, but set the worker's memory after created

        this.communicationManager = new CommunicationManager(this.p, this.privateMemoryMatrix);

        // set up the worker factory
        WorkerFactory workerFactory = new WorkerFactory(workerClass);
        workerFactory.init(this.communicationManager);

        // and create all the workers
        this.workers = new Matrix<>(this.p);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Worker w = workerFactory.createWorker(i, j, this.p, this.n, numComputationPhases, privateMemoryMatrix.get(i, j));
                this.workers.set(i, j, w);
            }
        }
    }

    /**
     * Does not delete existing memory content, just overrides what is provided.
     * @param memoryContent a map from string labels to numbers to distribute to each processing element
     */
    public void setPrivateMemory(Map<String, Matrix<Number>> memoryContent) {
        int matSizePerPE = n / p;

        // validate input
        for (String s : memoryContent.keySet()) {
            // mismatch in dimension
            if (memoryContent.get(s).size() != n) {
                throw new IllegalArgumentException(String.format("The passed initial memory content for label '%s' has dimension" +
                        "%d when dimension %d was expected.", s, memoryContent.get(s).size(), n));
            }
        }

        // initialize the private memory
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                for (String s : memoryContent.keySet()) {
                    for (int mi = 0; mi < matSizePerPE; mi++) {
                        for (int mj = 0; mj < matSizePerPE; mj++) {
                            privateMemoryMatrix.get(i, j).set(mi, mj, s, memoryContent.get(s).get(
                                    i * matSizePerPE + mi, j * matSizePerPE + mj
                            ));
                        }
                    }
                }
            }
        }
    }

    public Manager(int n, int numComputationPhases, Map<String, Matrix<Number>> initialMemoryContent,
                   Class<? extends Worker> workerClass) throws WorkerInstantiationException {
        this(n, n, numComputationPhases, initialMemoryContent, workerClass);
    }

    // TODO: resetMemory() which wipes everything

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
                    case INITIALISATION:
                        workerTask = this.workers.get(i, j).getInitialisationCallable();
                        break;
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
                LOGGER.log(Level.FINEST, "Phase {0}: Worker({1}, {2}) is being started.", new Object[]{phaseNumber, i, j});
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
     * Runs all the worker's work using an ExecutorService, blocking until all work has been completed. An exception
     * is thrown is any of the workers encounter a failure during execution.
     *
     * The work is done in the following order:
     * * All workers execute their INITIALISATION phase  in non-deterministic order
     * * All the workers are synchronised (we block until all workers has finished the above)
     * * Then the following is done once for each of the specified number of computation phases:
     *   * All the workers do their COMMUNICATION_BEFORE phase in non-deterministic order
     *   * All the workers are synchronised
     *   * All the worker do their COMPUTATION phase in non-deterministic order
     *   * All the workers are synchronised
     *   * All the workers do their COMMUNICATION_BEFORE phase in non-deterministic order
     *   * All the workers are synchronised
     *
     * @throws CommunicationChannelException if any of the workers attempt to use an already used communication channel
     *   during one of their communication phases
     * @throws WorkersFailedToCompleteException if any of the workers throw an exception during their computation phase.
     *   This exception is wrapped in a {@code WorkersFailedToCompleteException} and re-thrown. Examples include exceptions
     *   from from PrivateMemory in case access is attempted with a label that does not exist.
     */
    public void doWork() throws CommunicationChannelException, WorkersFailedToCompleteException {
        LOGGER.log(Level.INFO, "Manager is starting {0} phases of work with {1} workers.", new Object[]{this.numComputationPhases, this.p * this.p});

        // create the executor service which will manage the worker computation
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);

        List<Future<?>> workerFutures;

        // run the initialisation phase first of each worker
        LOGGER.log(Level.FINER, "Manager is running initialisation phase.");
        workerFutures = startWorkerExecution(-1, Worker.WorkerPhases.INITIALISATION);
        checkForWorkerFailure(workerFutures);

        for (int l = 0; l < this.numComputationPhases; l++) {
            if (l % 50 == 0) {
                LOGGER.info("Manager has completed " + l + " phases of work.");
            }
            // COMMUNICATION_BEFORE phase
            LOGGER.log(Level.FINER, "Manager is starting communicationBefore phase {0}", l);
            workerFutures = startWorkerExecution(l, Worker.WorkerPhases.COMMUNICATION_BEFORE);
            checkForWorkerFailure(workerFutures);
            this.communicationManager.flush();

            // COMPUTATION phase (no exception can be thrown here)
            LOGGER.log(Level.FINER, "Manager is starting computation phase {0}", l);
            // Why do we need to synchronise workers between computation and communication_after?:
            //   If we don't synchronise, we may get concurrent access to the workers' PrivateMemory through
            //   reading in both computation and communication. PrivateMemory does not guarantee thread-safe
            //   behaviour, so we must synchronise to avoid this.
            workerFutures = startWorkerExecution(l, Worker.WorkerPhases.COMPUTATION);
            checkForWorkerFailure(workerFutures);

            // COMMUNICATION_AFTER phase
            LOGGER.log(Level.FINER, "Manager is starting communicationAfter phase {0}", l);
            workerFutures = startWorkerExecution(l, Worker.WorkerPhases.COMMUNICATION_AFTER);
            checkForWorkerFailure(workerFutures);
            this.communicationManager.flush();
        }

        LOGGER.log(Level.INFO, "Manager has completed {0} phases of work.", this.numComputationPhases);
        this.executorService.shutdown();

        this.workHasBeenDone = true;
    }

    public Matrix<Number> getResult(String label) {
        return this.getResult(label, false);
    }

    /**
     * Returns the result of computation. A String label matching the memory location the workers store their result
     * in should be provided. This method creates a matrix of the Numbers found within each Worker's private memory
     * when accessed with the label {@code label}.
     *
     * @param label the string label. May be null, in which case an empty matrix is returned.
     * @return a Matrix of numbers of size n x n, independent of p
     */
    public Matrix<Number> getResult(String label, boolean asInt)  {
        if (!this.workHasBeenDone) {
            throw new IllegalStateException("The workers were not started, so result cannot be fetched");
        } else if (null == label) {
            return new Matrix<>(this.p);
        } else {
            // spread each PE's private memory across the result matrix
            int matSizePerPE = this.n / this.p;
            Matrix<Number> resultMatrix = new Matrix<>(this.n);
            for (int i = 0; i < this.p; i++) {
                for (int j = 0; j < this.p; j++) {
                    for (int mi = 0; mi < matSizePerPE; mi++) {
                        for (int mj = 0; mj < matSizePerPE; mj++) {
                            if (asInt) {
                                resultMatrix.set(i * matSizePerPE + mi, j * matSizePerPE + mj,
                                        this.workers.get(i, j).readInt(mi, mj, label));
                            } else {
                                resultMatrix.set(i * matSizePerPE + mi, j * matSizePerPE + mj,
                                        this.workers.get(i, j).readDouble(mi, mj, label));
                            }
                        }
                    }
                }
            }

            return resultMatrix;
        }
    }

    public int getProcessingElementGridSize() {
        return this.p;
    }

    public int getProblemSize() {
        return this.n;
    }

    public Worker getWorker(int i, int j) {
        return workers.get(i, j);
    }

    public void setWorker(int i, int j, Worker worker) {
        this.workers.set(i, j, worker);
    }

    public CommunicationManager getCommunicationManager() {
        return this.communicationManager;
    }

    /**
     * Tells the Manager and all of its Workers to use a different memory controller object. NOTE: This causes all
     * the Worker objects to be invalidated as all the workers are replaced by new ones, sharing the same private
     * memory. The reasoning for this is as follows: In the Worker implementation, the memory controller must be
     * final because it's used in a method that is called in the lambda functions defined in getComputationCallable etc.,
     * so we can't have a setter for the memoryController. Instead, we must recreate the worker with a new memoryController
     * reference, which is what we do here.
     *
     * @param communicationManager the new memory controller
     * @throws WorkerInstantiationException if the worker factory fails
     */
    public void setCommunicationManager(CommunicationManager communicationManager) throws WorkerInstantiationException {
        // set the self reference
        this.communicationManager = communicationManager;

        // TODO: maybe a setter in the Worker actually works as well, and the reason for it not working earlier
        //       was just the manager.Method fluke instead of this.Method on the TimedManager constructor?
        WorkerFactory workerFactory = new WorkerFactory(this.algorithm);
        workerFactory.init(communicationManager);
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Worker newWorker = workerFactory.createWorker(i, j, this.p, this.n, this.numComputationPhases,
                        this.workers.get(i, j).getPrivateMemory());
                this.workers.set(i, j, newWorker);
            }
        }
    }
}
