package timingAnalysis;

import memoryModel.CommunicationChannelCongestionException;
import work.Worker;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimedWorker extends Worker {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final ThreadMXBean threadMXBean;
    private long elapsedTime;
    private final Worker worker;

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
    }

    @Override
    public void initialise() {
        worker.initialise();
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
    protected Callable<Object> getComputationCallable(int l) {
        return () -> {
            LOGGER.log(Level.FINER, "Timed worker({0}, {1}) is starting computation phase {2}", new Object[]{i, j, l});

            // time the computation
            long elapsedTime = -1;

            // TODO: this does not work for generalized version. Suggested fix: Change the worker method for set
            //  and make it only do something if some flag enabled. Then disable the flag from here!
            if (this.average_compute) {
                // do the computation once, and reset memory, such that we don't get a cache miss
                double oldDist = this.getPrivateMemory().get(0, 0, "dist").doubleValue();
                computation(l);
                this.getPrivateMemory().set("dist", oldDist);

                long timeBefore = this.threadMXBean.getCurrentThreadCpuTime();
                for (int i = 0; i < this.average_num_iters; i++) {
                    computation(l);
                    // saving and resetting this means the worker does the same control flow each iteration
                    this.getPrivateMemory().set("dist", oldDist);
                }
                elapsedTime = (this.threadMXBean.getCurrentThreadCpuTime() - timeBefore) / average_num_iters;
            }
            // we need to do computation again anyways to save the result
            long timeBefore = this.threadMXBean.getCurrentThreadCpuTime();
            computation(l);
            long elapsedTimeNonAverage = this.threadMXBean.getCurrentThreadCpuTime() - timeBefore;

            // and save it for later
            this.elapsedTime += this.average_compute ? elapsedTime : elapsedTimeNonAverage;

            return null;
        };
    }

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
