import java.lang.management.ThreadMXBean;

public class Worker extends Thread {
    private final int i;
    private final int j;
    private final int n;
    private final boolean useSystemTime;
    private int result;
    private final int[][] matrix;
    private final ThreadMXBean bean;

    private long time;

    public long getTime() {
        return time;
    }

    public int getResult() {
        return result;
    }

    public Worker(int i, int j, int n, int[][] matrix, boolean useSystemTime, ThreadMXBean bean) {
        this.i = i;
        this.j = j;
        this.n = n;
        this.matrix = matrix;
        this.useSystemTime = useSystemTime;
        this.bean = bean;
    }

    @Override
    public void run() {
        // start timer
        if (this.useSystemTime) {
            this.time = System.nanoTime();
        } else {
            // does not include time spent on I/O if set to UserTime,
            // but that gives all 0s!
            this.time = bean.getCurrentThreadCpuTime();
        }
        // do computation
        int c = 0;
        for (int k = 0; k < n; k++) {
            c += matrix[i][k] * matrix[k][j];
        }
        this.result = c;
        // end timer
        if (this.useSystemTime) {
            this.time = System.nanoTime() - this.time;
        } else {
            this.time = bean.getCurrentThreadCpuTime() - this.time;
        }
    }
}
