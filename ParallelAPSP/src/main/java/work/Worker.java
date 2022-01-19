package work;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import org.junit.platform.commons.util.ExceptionUtils;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Worker implements Runnable {

    enum WorkerPhases {
        INITIALISATION,
        COMMUNICATION_BEFORE,
        COMPUTATION,
        COMMUNICATION_AFTER
    }

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    // The list of arguments to the abstract Worker class constructor
    static final Class[] workerConstructorParameterTypes = {
            Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
            PrivateMemory.class,  MemoryController.class
    };

    protected final int i;
    protected final int j;
    protected final int p;
    protected final int n;
    protected final int numPhases;

    private final PrivateMemory privateMemory;
    private final MemoryController memoryController;

    public Worker(Worker worker) {
        this.i = worker.i;
        this.j = worker.j;
        this.p = worker.p;
        this.n = worker.n;
        this.numPhases = worker.numPhases;

        this.privateMemory = worker.getPrivateMemory();
        this.memoryController = worker.memoryController;
    }

    /**
     * Constructs a Worker that handles the computation and communication on behalf of processing element (i, j).
     * The workers should all be given the same memoryController, cyclicBarrier and runExceptionHandler.
     *
     * @param i non-negative row ID of worker
     * @param j non-negative column ID of worker
     * @param p non-negative integer such that the number of workers == p ^ 2
     * @param numPhases number of computation phases to be performed during execution
     * @param privateMemory the worker's own private memory. May already contain values
     * @param memoryController a reference to a unique memory controller.
     *                         All workers should use the same memory controller
     */
    public Worker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, MemoryController memoryController) {
        this.i = i;
        this.j = j;
        this.p = p;
        this.n = n;
        this.numPhases = numPhases;

        this.privateMemory = privateMemory;
        this.memoryController = memoryController;

        // TODO: add method and assert for memory controller on size match
    }

    /**
     * Defines the work to be done by worker(i, j) prior to the first communication phase. In this method, calls to
     * memory writes such as {@link #store} are most suitable as this allows these labels to be references in all
     * iterations of the later communication and computation phases. No communication methods should be invoked from
     * this method, as this could give undefined behaviour.
     */
    abstract protected void initialise();

    /**
     * Defines the computation to be done by Worker(i, j) at computation phase l. Only the method
     * {@link #readDouble} should be used, not any of the communication methods. This is because the workers are
     * not synchronised between {@code computation} and {@code communicationAfter}, so there is not guarantee
     * on the communication order. Additionally, {@link CommunicationChannelCongestionException}s are not
     * handled appropriately if thrown from this method.
     *
     * @param l a non-negative integer representing number of computation phases already completed
     */
    abstract protected void computation(int l);

    /**
     * Defines the communication to be done by Worker(i, j) prior to computation phase l.
     *
     * @param l a non-negative integer representing the number of execution phases already completed
     * @throws CommunicationChannelCongestionException if different workers try to send data to the same
     * worker using the same communication method in the same phase.
     */
    abstract protected void communicationBefore(int l) throws CommunicationChannelCongestionException;

    /**
     * Defines the communication to be done by Worker(i, j) after computation phase l.
     *
     * @param l a non-negative integer representing the number of execution phases already completed
     * @throws CommunicationChannelCongestionException if different workers try to send data to the same
     * worker using the same communication method in the same phase.
     */
    abstract protected void communicationAfter(int l) throws CommunicationChannelCongestionException;

    protected Number read(String label) {
        return this.readDouble(0, 0, label);
    }

    protected int readInt(String label) {
        return this.readInt(0, 0, label);
    }

    protected double readDouble(String label) {
        return this.readDouble(0, 0, label);
    }

    // TODO: add try catch and send Exception from computation if e.g. get NullPointerException when reading
    //       label that does not exist in the PrivateMemory. No longer necessary because unchecked thrown in PrivateMemory
    //       and caught and reported in Manager!
    protected Number read(int mi, int mj, String label) {
        return this.privateMemory.get(mi, mj, label);
    }

    protected int readInt(int mi, int mj, String label) {
        return this.read(mi, mj, label).intValue();
    }

    protected double readDouble(int mi, int mj, String label) {
        return this.read(mi, mj, label).doubleValue();
    }

    protected void store(String label, Number value) {
        this.store(0, 0, label, value);
    }

    protected void store(int mi, int mj, String label, Number value) {
        this.privateMemory.set(mi, mj, label, value);
    }

    protected void send(int i, int j, Number value) throws CommunicationChannelCongestionException {
        this.memoryController.sendData(this.i, this.j,  i, j, value);
    }

    protected void receive(String label) {
        this.receive(0, 0, label);
    }

    protected void receive(int mi, int mj, String label) {
        this.memoryController.receiveData(this.i, this.j, mi, mj, label);
    }

    protected void broadcastRow(Number value) throws CommunicationChannelCongestionException {
        this.memoryController.broadcastRow(this.i, this.j, value);
    }

    protected void broadcastCol(Number value) throws CommunicationChannelCongestionException {
        this.memoryController.broadcastCol(this.i, this.j, value);
    }

    protected void receiveRowBroadcast(String label) {
        this.receiveRowBroadcast(0, 0, label);
    }

    protected void receiveRowBroadcast(int mi, int mj, String label) {
        this.memoryController.receiveRowBroadcast(this.i, this.j, mi, mj, label);
    }

    protected void receiveColBroadcast(String label) {
        this.receiveColBroadcast(0, 0, label);
    }

    protected void receiveColBroadcast(int mi, int mj, String label) {
        this.memoryController.receiveColBroadcast(this.i, this.j, mi, mj, label);
    }

    Callable<Object> getInitialisationCallable() {
        return () -> {
            LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting initialisation phase.", new Object[]{i, j});
            initialise();
            return null;
        };
    }

    Callable<Object> getComputationCallable(int l) {
        // TODO: add timers
        return () -> {
            LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting computation phase {2}", new Object[]{i, j, l});
            computation(l);
            return null;
        };
    }

    Callable<Object> getCommunicationBeforeCallable(int l) {
        return () -> {
            LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting communicationBefore phase {2}", new Object[]{i, j, l});
            communicationBefore(l);
            return null;
        };
    }

    Callable<Object> getCommunicationAfterCallable(int l) {
        return () -> {
            LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting communicationAfter phase {2}", new Object[]{i, j, l});
            communicationAfter(l);
            return null;
        };
    }

    @Override
    @Deprecated
    public void run() {
        LOGGER.warning("Using a worker as Runnable should only be used when testing a single worker."
            + " To run synchronised workers in parallel, the getCallable methods with a ExecutorService should be used instead.");

        for (int l = 0; l < this.numPhases; l++) {
            // Communication before
            try {
                this.communicationBefore(l);
            } catch (CommunicationChannelCongestionException e) {
                LOGGER.log(Level.WARNING, "Worker({0}, {1}) encountered an error in communicationBefore phase {2}: {3}",
                        new Object[]{this.i, this.j, l, ExceptionUtils.readStackTrace(e)});
                return;
            }

            // Computation
            this.computation(l);

            // Communication after
            try {
                this.communicationAfter(l);
            } catch (CommunicationChannelCongestionException e) {
                LOGGER.log(Level.WARNING,"Worker({0}, {1}) encountered an error in communicationAfter phase {2}: {3}",
                        new Object[]{this.i, this.j, l, ExceptionUtils.readStackTrace(e)});
                return;
            }
        }
    }

    public PrivateMemory getPrivateMemory() {
        return privateMemory;
    }
}
