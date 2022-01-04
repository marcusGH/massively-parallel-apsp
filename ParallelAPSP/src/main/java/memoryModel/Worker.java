package memoryModel;

import org.junit.platform.commons.util.ExceptionUtils;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Worker implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    protected final int i;
    protected final int j;
    protected final int p;
    private final int numPhases;
    private final PrivateMemory privateMemory;
    private final MemoryController memoryController;
    private final CyclicBarrier cyclicBarrier;
    private final Runnable runExceptionHandler;

//    public Worker(int i, int j, int n, PrivateMemory<T> privateMemory, MemoryController<T> memoryController, CyclicBarrier cyclicBarrier) {
//        // if the number of phases is not specified, we default to n, the number of rows and columns
//        this(i, j, n, n, privateMemory, memoryController, cyclicBarrier);
//    }


    public Worker(int i, int j, int p, int numPhases, PrivateMemory privateMemory,
                     MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        this.i = i;
        this.j = j;
        this.p = p;
        // number of computation phases to execute
        this.numPhases = numPhases;

        this.privateMemory = privateMemory;
        this.memoryController = memoryController;
        this.cyclicBarrier = cyclicBarrier;
        this.runExceptionHandler = runExceptionHandler;

        // The cyclic barrier's number of parties must match the number of workers
        assert this.cyclicBarrier.getParties() == p * p;
        // TODO: add method and assert for memory controller
    }

    abstract void computation(int l);

    abstract void communicationBefore(int l) throws CommunicationChannelCongestionException;

    abstract void communicationAfter(int l) throws CommunicationChannelCongestionException;

//    static Worker<T> workerSupplier(int i, int j, int p, int numPhases, PrivateMemory<T> privateMemory,
//                                      MemoryController<T> memoryController, CyclicBarrier cyclicBarrier,
//                                      Runnable runExceptionHandler);

    // TODO: when refactoring memoryModel package, would it be possible to only allow PrivateMemory read-methods to
    //       be accessible publically? If so, it owuld be better to remove all read methods here and give a protected
    //       privateMemory reference to the programmer instead. That way, we don't need to replicate all the functionality
    //       here. ~~Additionally, when subclassing privateMemory, we can use new methods as well~~ No, we can't.
    protected double read(String label) {
        return this.read(0, 0, label);
    }

    protected double read(int mi, int mj, String label) {
        return this.privateMemory.getDouble(mi, mj, label);
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

    @Override
    public void run() {
        // TODO: add timers
        for (int l = 0; l < this.numPhases; l++) {
            // Communication before
            try {
                LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting communicationBefore phase {2}", new Object[]{i, j, l});
                this.communicationBefore(l);
            } catch (CommunicationChannelCongestionException e) {
                LOGGER.log(Level.WARNING, "Worker({0}, {1}) encountered an error in communicationBefore phase {2}: {3}",
                        new Object[]{this.i, this.j, l, ExceptionUtils.readStackTrace(e)});
                this.runExceptionHandler.run();
            }

            // "Communication before"-synchronisation
            try {
                // If the thread has its interrupted status set on entry to this method or is interrupted while waiting,
                // the barrier enters a broken state. This means that all threads already waiting or are arriving at a
                // later time will throw an Exception, allowing us to gracefully break all running threads out of the
                // for-loop
                this.cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                LOGGER.log(Level.WARNING, "Worker({0}, {1}): Cyclic barrier is broken, so stopping computation.", new Object[]{i, j});
                break;
            }

            // Computation
            LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting computation phase {2}", new Object[]{i, j, l});
            if (!Thread.currentThread().isInterrupted()) {
                this.computation(l);
            }

            // Communication after
            try {
                LOGGER.log(Level.FINER, "Worker({0}, {1}) is starting communicationAfter phase {2}", new Object[]{i, j, l});
                this.communicationAfter(l);
            } catch (CommunicationChannelCongestionException e) {
                LOGGER.log(Level.WARNING,"Worker({0}, {1}) encountered an error in communicationAfter phase {2}: {3}",
                        new Object[]{this.i, this.j, l, ExceptionUtils.readStackTrace(e)});
                this.runExceptionHandler.run();
            }

            // "Communication after"-synchronisation
            try {
                // If the thread has its interrupted status set on entry to this method or is interrupted while waiting,
                // the barrier enters a broken state. This means that all threads already waiting or are arriving at a
                // later time will throw an Exception, allowing us to gracefully break all running threads out of the
                // for-loop
                this.cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                LOGGER.log(Level.WARNING, "Worker({0}, {1}): Cyclic barrier is broken, so stopping computation.", new Object[]{i, j});
                break;
            }
        }
    }

    public PrivateMemory getPrivateMemory() {
        return privateMemory;
    }
}
