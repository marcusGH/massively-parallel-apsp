package work;

import memoryModel.CommunicationChannelCongestionException;
import memoryModel.CommunicationManager;
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
            PrivateMemory.class,  CommunicationManager.class
    };

    protected final int i;
    protected final int j;
    protected final int p;
    protected final int n;
    protected final int numPhases;

    private final PrivateMemory privateMemory;
    private final CommunicationManager communicationManager;

    public Worker(Worker worker) {
        this.i = worker.i;
        this.j = worker.j;
        this.p = worker.p;
        this.n = worker.n;
        this.numPhases = worker.numPhases;

        this.privateMemory = worker.getPrivateMemory();
        this.communicationManager = worker.communicationManager;
    }

    /**
     * Constructs a Worker that handles the computation and communication on behalf of processing element (i, j).
     * The workers should all be given the same communicationManager, cyclicBarrier and runExceptionHandler.
     *
     * @param i non-negative row ID of worker
     * @param j non-negative column ID of worker
     * @param p non-negative integer such that the number of workers == p ^ 2
     * @param numPhases number of computation phases to be performed during execution
     * @param privateMemory the worker's own private memory. May already contain values
     * @param communicationManager a reference to a unique communication manager.
     *                         All workers should use the same communication manager
     */
    public Worker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory, CommunicationManager communicationManager) {
        this.i = i;
        this.j = j;
        this.p = p;
        this.n = n;
        this.numPhases = numPhases;

        this.privateMemory = privateMemory;
        this.communicationManager = communicationManager;
    }

    /**
     * Defines the work to be done by worker(i, j) prior to the first communication phase. In this method, calls to
     * memory writes such as {@link #store} are most suitable as this allows these labels to be references in all
     * iterations of the later communication and computation phases. No communication methods should be invoked from
     * this method, as this could give undefined behaviour.
     */
    abstract public void initialisation();

    /**
     * Defines the computation to be done by Worker(i, j) at computation phase l. Only the method
     * {@link #readDouble} should be used, not any of the communication methods. This is because the workers are
     * not synchronised between {@code computation} and {@code communicationAfter}, so there is not guarantee
     * on the communication order. Additionally, {@link CommunicationChannelCongestionException}s are not
     * handled appropriately if thrown from this method.
     *
     * @param l a non-negative integer representing number of computation phases already completed
     */
    abstract public void computation(int l);

    /**
     * Defines the communication to be done by Worker(i, j) prior to computation phase l.
     *
     * @param l a non-negative integer representing the number of execution phases already completed
     * @throws CommunicationChannelCongestionException if different workers try to send data to the same
     * worker using the same communication method in the same phase.
     */
    abstract public void communicationBefore(int l) throws CommunicationChannelCongestionException;

    /**
     * Defines the communication to be done by Worker(i, j) after computation phase l.
     *
     * @param l a non-negative integer representing the number of execution phases already completed
     * @throws CommunicationChannelCongestionException if different workers try to send data to the same
     * worker using the same communication method in the same phase.
     */
    abstract public void communicationAfter(int l) throws CommunicationChannelCongestionException;

    // =============== Methods for reading private memory ===================

    protected boolean presentInMemory(String label) {
        return this.presentInMemory(0, 0, label);
    }

    protected boolean presentInMemory(int mi, int mj, String label) {
        return this.privateMemory.contains(mi, mj, label);
    }

    protected Number read(String label) {
        return this.readDouble(0, 0, label);
    }

    protected Number read(int mi, int mj, String label) {
        return this.privateMemory.get(mi, mj, label);
    }

    protected int readInt(String label) {
        return this.readInt(0, 0, label);
    }

    protected int readInt(int mi, int mj, String label) {
        return this.read(mi, mj, label).intValue();
    }

    protected double readDouble(String label) {
        return this.readDouble(0, 0, label);
    }

    protected double readDouble(int mi, int mj, String label) {
        return this.read(mi, mj, label).doubleValue();
    }

    // ================ Methods for storing to private memory ==================

    protected void store(String label, Number value) {
        this.store(0, 0, label, value);
    }

    // must be public so that TimedWorker in timingAnalysis class can override this method using functionality of its
    //   worker reference
    public void store(int mi, int mj, String label, Number value) {
        this.privateMemory.set(mi, mj, label, value);
    }

    // ================ Methods for point to point communication ================

    protected void send(int i, int j, Number value) throws CommunicationChannelCongestionException {
        this.communicationManager.sendData(this.i, this.j,  i, j, value);
    }

    protected void receive(String label) {
        this.receive(0, 0, label);
    }

    protected void receive(int mi, int mj, String label) {
        this.communicationManager.receiveData(this.i, this.j, mi, mj, label);
    }

    // ================ Methods for broadcast communication ==================

    protected void broadcastRow(Number value) throws CommunicationChannelCongestionException {
        this.communicationManager.broadcastRow(this.i, this.j, value);
    }

    protected void broadcastCol(Number value) throws CommunicationChannelCongestionException {
        this.communicationManager.broadcastCol(this.i, this.j, value);
    }

    protected void receiveRowBroadcast(String label) {
        this.receiveRowBroadcast(0, 0, label);
    }

    protected void receiveRowBroadcast(int mi, int mj, String label) {
        this.communicationManager.receiveRowBroadcast(this.i, this.j, mi, mj, label);
    }

    protected void receiveColBroadcast(String label) {
        this.receiveColBroadcast(0, 0, label);
    }

    protected void receiveColBroadcast(int mi, int mj, String label) {
        this.communicationManager.receiveColBroadcast(this.i, this.j, mi, mj, label);
    }

    // ================= Methods for interaction with Manager ==============

    Callable<Object> getInitialisationCallable() {
        return () -> {
            LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting initialisation phase.", new Object[]{i, j});
            initialisation();
            return null;
        };
    }

    // Must be protected such that TimedWorker in a different package can override this and decorate it
    //   with timing analysis functionality
    protected Callable<Object> getComputationCallable(int l) {
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

    // ==================== Other utilities =====================

    public PrivateMemory getPrivateMemory() {
        return privateMemory;
    }

    @Override
    // @Deprecated
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

}
