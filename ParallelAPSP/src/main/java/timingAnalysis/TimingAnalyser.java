package timingAnalysis;

import matrixMultiplication.FoxOtto;
import memoryModel.CommunicationChannelException;
import memoryModel.topology.SquareGridTopology;
import util.Matrix;
import work.Manager;
import work.WorkerInstantiationException;
import work.WorkersFailedToCompleteException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class TimingAnalyser {
    // TODO: change everything to nanoseconds! We are getting overflows :/

    // cat /proc/cpuinfo | grep GHz
    public static final double ACER_NITRO_CPU_CYCLES_PER_NANOSECOND = 2.3; // 2.3GHz
    public static final int POINT_TO_POINT_SEND_CLOCK_CYCLES = 75;
    public static final int BROADCAST_CLOCK_CYCLES = POINT_TO_POINT_SEND_CLOCK_CYCLES;

    private final TimedManager timedManager;

    // measured in seconds
    private final double broadcast_latency;
    private final double p2p_latency;
    // measures in bytes per second
    private final double p2p_bandwidth;
    private final double broadcast_bandwidth;
    // measures in bytes
    private final int word_size = 4;

    /**
     * @param timedManager               a manager where the work has already completed
     * @param cpu_cps                    the cycles per second of the CPU of each processing element
     * @param p2p_cycles                 the number of cycles to send a message to an adjacent node
     * @param broadcast_cycles           the number of cycles to broadcast a message across a row or column
     * @param p2p_bandwidth_bytes_per_cycle       The bandwidth of core-to-core communication, measured in Bytes per cycle
     * @param broadcast_bandwidth_bytes_per_cycle the bandwidth of broadcasting, measured in Bytes per cycle
     */
    public TimingAnalyser(TimedManager timedManager, double cpu_cps, int p2p_cycles, int broadcast_cycles,
                          int p2p_bandwidth_bytes_per_cycle, int broadcast_bandwidth_bytes_per_cycle) {
        this.timedManager = timedManager;
        // find latency
        this.broadcast_latency = (double) broadcast_cycles / cpu_cps;
        this.p2p_latency = (double) p2p_cycles / cpu_cps;
        // find bandwidth
        this.p2p_bandwidth = p2p_bandwidth_bytes_per_cycle * cpu_cps;
        this.broadcast_bandwidth = broadcast_bandwidth_bytes_per_cycle * cpu_cps;

        // TODO: add other constructor for this variant
    }

    private LongSummaryStatistics getStatsSummary(Matrix<Long> matrix) {
        return matrix.toList().stream().collect(
                LongSummaryStatistics::new,
                LongSummaryStatistics::accept,
                LongSummaryStatistics::combine);
    }

    private double getDeviation(Matrix<? extends Number> matrix) {
        double sum = 0.0;
        double mean = matrix.toList().stream().mapToDouble(Number::doubleValue).average().orElse(-1);
        if (mean == -1) {
            throw new RuntimeException("Provided matrix is empty");
        }
        for (Number n : matrix.toList()) {
            sum += Math.pow(Math.abs(n.doubleValue() - mean), 2);
        }

        return Math.sqrt(sum / (matrix.size() * matrix.size()));
    }

    public void getComputationTimes() {
        List<Matrix<Long>> p2pTimes = this.timedManager.getComputationTimes();
        for (int i = 0; i < p2pTimes.size(); i++) {
            LongSummaryStatistics longSummaryStatistics = getStatsSummary(p2pTimes.get(i));
            System.out.println(p2pTimes.get(i).toList());
            System.out.println("In phase " + i + " average compute=" + longSummaryStatistics.getAverage() + " std+" + getDeviation(p2pTimes.get(i)));
        }
    }

    private double get_send_time(int num_words, boolean is_broadcast) {
        // special case
        if (num_words == 0) {
            return 0.0;
        }
        if (is_broadcast) {
            return this.broadcast_latency + (num_words * this.word_size / this.broadcast_bandwidth);
        } else {
            return this.p2p_latency + (num_words * this.word_size / this.p2p_bandwidth);
        }
    }

    private List<List<Double>> getPointToPointCommunicationTimes() {
        return this.timedManager.getPointToPointCommunicationTimes().stream()
                .map(Matrix::toList)
                // Find time it takes to do this communication from each PE
                .map(l -> l.stream()
                        .mapToDouble(t -> get_send_time(t, false)).boxed()
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private List<List<Double>> getBroadcastCommunicationTimes(List<List<Integer>> broadcastCounts) {
        return broadcastCounts.stream()
                .map(l -> l.stream()
                        .mapToDouble(t -> get_send_time(t, true)).boxed()
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private List<List<Double>> getComputationTimes2() {
        return this.timedManager.getComputationTimes().stream()
                .map(Matrix::toList)
                .map(l -> l.stream()
                        .mapToDouble(Long::doubleValue).boxed()
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public void saveTimings(String file_basename) throws IOException {
        Path computation_file = Paths.get(file_basename + "_computation.csv");
        Path communication_file = Paths.get(file_basename + "_communication.csv");

        List<List<Double>> computationTimes = getComputationTimes2();
        List<List<Double>> p2pTimes = getPointToPointCommunicationTimes();
        List<List<Double>> rowTimes = getBroadcastCommunicationTimes(this.timedManager.getRowBroadcastCommunicationTimes());
        List<List<Double>> colTimes = getBroadcastCommunicationTimes(this.timedManager.getColBroadcastCommunicationTimes());

        // create the content to write
        List<String> computationLines = new ArrayList<>();
        for (int i = 0; i < computationTimes.size(); i++) {
            String sb = i + ", " + computationTimes.get(i).stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
            computationLines.add(sb);
        }
        List<String> communicationLines = new ArrayList<>();
        for (int i = 0; i < p2pTimes.size(); i++) {
            communicationLines.add(i + ", " + p2pTimes.get(i).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
            communicationLines.add(i + ", " + rowTimes.get(i).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
            communicationLines.add(i + ", " + colTimes.get(i).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
        }

        // write the content
        Files.write(computation_file, computationLines, StandardCharsets.UTF_8);
        Files.write(communication_file, communicationLines, StandardCharsets.UTF_8);
    }

    // TODO: it would be better for this class to just print out lists of numbers, one line for each computation phase
    //       and there's separate files for each type of timing. We then do the aggregation with mean, std, the plotting
    //       etc. in python. It can also be useful to run the algorithm many times, producing many files such that we can
    //       average across all "phase 1"s in python when doing the analysis.

    public static void main(String[] args) {
        final int INF = Integer.MAX_VALUE;

        // the graph we're working with
        Number[][] adjacencyGrid = {
                {0,   6,    2,   3,  INF, INF, INF},
                {INF, 0  , INF, INF,  1 , INF, INF},
                {INF, INF, 0  , INF, INF,  2,   1 },
                {INF, INF, INF,  0 , INF, INF,  2 },
                {INF, INF, INF, INF,  0 , INF, INF},
                {INF,  1 , INF, INF, INF,  0 , INF},
                {INF, INF, INF, INF, INF, INF,  0 },
        };
        Matrix<Number> adjMatrix = new Matrix<Number>(7, adjacencyGrid);
        // setup the predecessor matrix
        Matrix<Number> predMatrix = new Matrix<>(7);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (INF == adjMatrix.get(i, j).intValue()) {
                    predMatrix.set(i, j, j);
                } else {
                    predMatrix.set(i, j, i);
                }

            }
        }
        // and put the two as initial memory content
        Map<String, Matrix<Number>> initialMemory = new HashMap<>();
        initialMemory.put("A", adjMatrix);
        initialMemory.put("B", adjMatrix);
        initialMemory.put("P", predMatrix);

        // create the timing manager
        TimedManager timedManager;
        try {
            Manager manager = new Manager(7, 7, initialMemory, FoxOtto.class);
            timedManager = new TimedManager(manager, SquareGridTopology::new);
            timedManager.enableFoxOttoTimeAveraging(10000);
        } catch (WorkerInstantiationException e) {
            e.printStackTrace();
            return;
        }

        try {
            // compute the next matrix
            timedManager.doWork();
        } catch (CommunicationChannelException | WorkersFailedToCompleteException e) {
            e.printStackTrace();
            return;
        }

        // print statistics
        TimingAnalyser timingAnalyser = new TimingAnalyser(timedManager, TimingAnalyser.ACER_NITRO_CPU_CYCLES_PER_NANOSECOND,
                TimingAnalyser.POINT_TO_POINT_SEND_CLOCK_CYCLES, TimingAnalyser.BROADCAST_CLOCK_CYCLES,
                64, 64);

        timingAnalyser.getComputationTimes();
        try {
            timingAnalyser.saveTimings("../test-file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
