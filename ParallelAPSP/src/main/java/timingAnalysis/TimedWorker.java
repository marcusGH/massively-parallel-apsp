package timingAnalysis;

import memoryModel.CommunicationChannelCongestionException;
import work.Worker;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimedWorker extends Worker {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final ThreadMXBean threadMXBean;
    private long elapsedTime;
    private final Worker worker;

    private boolean readonly;
    private boolean average_compute;
    private int average_num_iters;

    /**
     * Decorator pattern on worker ...
     * @param worker a Worker object of some subtype of Worker
     */
    public TimedWorker(Worker worker) {
        super(worker);
        this.worker = worker;
        // there's only one instance of this, so all TimedWorkers hold the same reference
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.elapsedTime = 0L;
        this.average_compute = false;
        this.readonly = false;
    }

    @Override
    public void initialisation() {
        worker.initialisation();
    }

    @Override
    public void computation(int l) {
        worker.computation(l);
    }

    @Override
    public void communicationBefore(int l) throws CommunicationChannelCongestionException {
        worker.communicationBefore(l);
    }

    @Override
    public void communicationAfter(int l) throws CommunicationChannelCongestionException {
        worker.communicationAfter(l);
    }

    @Override
    public void store(int mi, int mj, String label, Number value) {
        // when averaging over many iterations, we want to computation to be the same
        //  in all of them, so do this to prevent the worker's state from changing
        if (!this.readonly) {
            worker.store(mi, mj, label, value);
        }
    }

    @Override
    protected Callable<Object> getComputationCallable(int l) {
        return () -> {
            LOGGER.log(Level.FINER, "Timed worker({0}, {1}) is starting computation phase {2}", new Object[]{i, j, l});

            // time the computation
            long elapsedTime = -1;

            // while running through the worker's computation many times, we make its memory read only because
            //   that way it always perform the same computation in each iteration. For example, it won't only
            //   take the branch on the first iteration
            if (this.average_compute) {
                this.readonly = true;
                // do the computation once, so that necessary variables are close in cache
                computation(l);

                // average the computation time over many runs
                long timeBefore = this.threadMXBean.getCurrentThreadCpuTime();
                for (int i = 0; i < this.average_num_iters; i++) {
                    // won't change internal state because we are in readonly
                    computation(l);
                }
                elapsedTime = (this.threadMXBean.getCurrentThreadCpuTime() - timeBefore) / average_num_iters;
            }
            this.readonly = false;

            // we need to do computation again anyways to save the result
            long timeBefore = System.nanoTime();
            computation(l);
            long elapsedTimeNonAverage = System.nanoTime() - timeBefore;

            // and save it for later
            this.elapsedTime += this.average_compute ? elapsedTime : elapsedTimeNonAverage;

            return null;
        };
    }

    /**
     * @param num_iterations number of times to repeat each phase
     */
    void enableAverageComputeTimes(int num_iterations) {
        this.average_compute = true;
        this.average_num_iters = num_iterations;
    }

    long getElapsedTime() {
        return this.elapsedTime;
    }

    void resetElapsedTime() {
        this.elapsedTime = 0L;
    }
}
