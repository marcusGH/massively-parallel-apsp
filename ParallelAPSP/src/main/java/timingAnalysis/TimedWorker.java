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
    private final List<Long> elapsedTimes;
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
        this.elapsedTimes = new ArrayList<>();
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
            if (this.average_compute) {
                // saving and resetting this means the worker does the same control flow each iteration
                double oldDist = this.getPrivateMemory().get(0, 0, "dist").doubleValue();
                long timeBefore = this.threadMXBean.getCurrentThreadCpuTime();
                for (int i = 0; i < this.average_num_iters; i++) {
                    computation(l);
                    this.getPrivateMemory().set("dist", oldDist);
                }
                elapsedTime = (this.threadMXBean.getCurrentThreadCpuTime() - timeBefore) / average_num_iters;
            }
            // we need to do computation again anyways to save the result
            long timeBefore = this.threadMXBean.getCurrentThreadCpuTime();
            computation(l);
            long elapsedTimeNonAverage = this.threadMXBean.getCurrentThreadCpuTime() - timeBefore;

            // and save it for later
            this.elapsedTimes.add(this.average_compute ? elapsedTime : elapsedTimeNonAverage);

            return null;
        };
    }

    void enableAverageComputeTimes(int num_iterations) {
        this.average_compute = true;
        this.average_num_iters = num_iterations;
    }

    List<Long> getElapsedTimes() {
        return elapsedTimes;
    }
}
