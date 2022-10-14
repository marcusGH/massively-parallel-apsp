import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ParallelMatrixMultiplication {
    // in actual implementation, would have Matrix class
    private final int[][] matrix;
    private final int n;
    private final ThreadMXBean bean;

    public void square(String filename, boolean useSystemTime, boolean printResult) throws InterruptedException {
        int[][] res = new int[n][];
        for (int i = 0; i < n; i++) {
            res[i] = new int[n];
        }

        // create the thread pool
        Worker[][] threads = new Worker[n][];
        for (int i = 0; i < n; i++) {
            threads[i] = new Worker[n];
            for (int j = 0; j < n; j++) {
                threads[i][j] = new Worker(i, j, n, matrix, useSystemTime, bean);
            }
        }

        // start them all off
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                threads[i][j].start();
            }
        }
        // collect the results
        ArrayList<Long> times = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                threads[i][j].join();
                times.add(threads[i][j].getTime());
                res[i][j] = threads[i][j].getResult();
            }
        }

        if (printResult) {
            System.out.println(getString(res));
        }
        StringBuilder s = new StringBuilder(String.valueOf(times.get(0)));
        for (int i = 1; i < times.size(); i++) {
            s.append(",").append(times.get(i));
        }
        // save the array to file
        try {
            File f = new File(filename);
            if (f.createNewFile()) {
                System.out.println("Created file: " + f.getName());
            } else {
                System.out.println("File already exists.");
            }

            FileWriter fw = new FileWriter(filename);
            fw.write(s.append("\n").toString());
            fw.close();
            System.out.println("Successfully wrote to file");
        } catch (IOException e) {
            System.out.println("Could not create file or failed to write to file.");
        }
    }

    private String getString(int[][] arr) {
        StringBuilder s = new StringBuilder("");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                s.append(arr[i][j]).append(" ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    /**
     * Constructs a random matrix of size n x n
     *
     * @param n the dimension
     */
    public ParallelMatrixMultiplication(int n) {
        int array[][] = new int[n][];

        for (int i = 0; i < n; i++) {
            int col[] = new int[n];
            for (int j = 0; j < n; j++) {
                col[j] = ThreadLocalRandom.current().nextInt(0, 10);
            }
            array[i] = col;
        }

        this.matrix = array;
        this.n = n;
        this.bean = ManagementFactory.getThreadMXBean();

        System.out.println("Created matrix:\n" + getString(this.matrix));
    }

    public static void main(String[] args) {
        int n = 50;
        ParallelMatrixMultiplication pmm = new ParallelMatrixMultiplication(n);
        try {
            pmm.square("execution-time-bean-time-" + n + ".csv", false, false);
        } catch (InterruptedException e) {
            System.out.println("The program was interrupted");
        }
    }
}
