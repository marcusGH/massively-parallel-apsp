package memoryModel;

import org.junit.platform.commons.util.ExceptionUtils;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public abstract class Worker<T> implements Runnable {
    protected final int i;
    protected final int j;
    protected final int p;
    private final int numPhases;
    private final PrivateMemory<T> privateMemory;
    private final MemoryController<T> memoryController;
    private final CyclicBarrier cyclicBarrier;
    private final Runnable runExceptionHandler;

//    public Worker(int i, int j, int n, PrivateMemory<T> privateMemory, MemoryController<T> memoryController, CyclicBarrier cyclicBarrier) {
//        // if the number of phases is not specified, we default to n, the number of rows and columns
//        this(i, j, n, n, privateMemory, memoryController, cyclicBarrier);
//    }


    protected Worker(int i, int j, int p, int numPhases, PrivateMemory<T> privateMemory,
                     MemoryController<T> memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
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

    protected T read(String label) {
        return this.read(0, 0, label);
    }

    protected T read(int mi, int mj, String label) {
        return this.privateMemory.get(mi, mj, label);
    }

    protected void send(int i, int j, T value) throws CommunicationChannelCongestionException {
        this.memoryController.sendData(this.i, this.j,  i, j, value);
    }

    protected void receive(String label) {
        this.receive(0, 0, label);
    }

    protected void receive(int mi, int mj, String label) {
        this.memoryController.receiveData(this.i, this.j, mi, mj, label);
    }

    protected void broadcastRow(T value) throws CommunicationChannelCongestionException {
        this.memoryController.broadcastRow(this.i, this.j, value);
    }

    protected void broadcastCol(T value) throws CommunicationChannelCongestionException {
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
            try {
                this.communicationBefore(l);
                this.cyclicBarrier.await();
            } catch (CommunicationChannelCongestionException | InterruptedException | BrokenBarrierException e) {
                System.err.println(String.format("Worker(%d, %d) encountered an error in communicationBefore phase %d: %s. %s",
                        this.i, this.j, l, e.getMessage(), ExceptionUtils.readStackTrace(e)));
                this.runExceptionHandler.run();
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            } else {
                this.computation(l);
            }

            try {
                this.communicationAfter(l);
                this.cyclicBarrier.await();
            } catch (CommunicationChannelCongestionException | InterruptedException | BrokenBarrierException e) {
                System.err.println(String.format("Worker(%d, %d) encountered an error in communicationAfter phase %d: %s. %s",
                        this.i, this.j, l, e.getMessage(), ExceptionUtils.readStackTrace(e)));
                this.runExceptionHandler.run();
            }

            if (Thread.currentThread().isInterrupted()) break;
        }
    }

    public PrivateMemory<T> getPrivateMemory() {
        return privateMemory;
    }
}
