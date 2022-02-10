package timingAnalysis;

import util.Matrix;

import java.util.List;
import java.util.stream.Collectors;

public class MultiprocessorAttributes {

    // cat /proc/cpuinfo | grep GHz
    public static final double ACER_NITRO_CPU_CYCLES_PER_NANOSECOND = 2.3; // 2.3GHz
    public static final int POINT_TO_POINT_SEND_CLOCK_CYCLES = 75;
    public static final int BROADCAST_CLOCK_CYCLES = POINT_TO_POINT_SEND_CLOCK_CYCLES;

    private static final int WORD_SIZE = 4; // 32 bits
    private static final long SEC_TO_NANO = 1_000_000_000;

    // measured in seconds
    private final double broadcast_latency;
    private final double p2p_latency;
    // measures in bytes per second
    private final double p2p_bandwidth;
    private final double broadcast_bandwidth;

    /**
     * @param cpu_cps                    the cycles per second of the CPU of each processing element
     * @param p2p_cycles                 the number of cycles to send a message to an adjacent node
     * @param broadcast_cycles           the number of cycles to broadcast a message across a row or column
     * @param p2p_bandwidth_bytes_per_cycle       The bandwidth of core-to-core communication, measured in Bytes per cycle
     * @param broadcast_bandwidth_bytes_per_cycle the bandwidth of broadcasting, measured in Bytes per cycle
     */
    public MultiprocessorAttributes(double cpu_cps, int p2p_cycles, int broadcast_cycles,
                          int p2p_bandwidth_bytes_per_cycle, int broadcast_bandwidth_bytes_per_cycle) {
        // find latency
        this.broadcast_latency = (double) broadcast_cycles / cpu_cps;
        this.p2p_latency = (double) p2p_cycles / cpu_cps;
        // find bandwidth
        this.p2p_bandwidth = p2p_bandwidth_bytes_per_cycle * cpu_cps;
        this.broadcast_bandwidth = broadcast_bandwidth_bytes_per_cycle * cpu_cps;
    }

    /**
     * Returns time in nanoseconds
     *
     * @param num_words
     * @param is_broadcast
     * @param no_latency
     * @return
     */
    public double getSendTime(int num_words, boolean is_broadcast, boolean no_latency) {
        // special case
        if (num_words == 0) {
            return 0L;
        }
        if (is_broadcast) {
            double latency = no_latency ? 0 : this.broadcast_latency;
            return (latency + (num_words * WORD_SIZE / this.broadcast_bandwidth)) * SEC_TO_NANO;
        } else {
            double latency = no_latency ? 0 : this.p2p_latency;
            return (latency + (num_words * WORD_SIZE / this.p2p_bandwidth)) * SEC_TO_NANO;
        }
    }

    public double getSendTime(int num_words, boolean is_broadcast) {
        return getSendTime(num_words, is_broadcast, false);
    }


}
