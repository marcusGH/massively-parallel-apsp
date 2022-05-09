package timingAnalysis;

public class MultiprocessorAttributes {

    // cat /proc/cpuinfo | grep GHz
    public static final double ACER_NITRO_CPU_CYCLES_PER_NANOSECOND = 2.3; // 2.3GHz
    public static final double ACER_NITRO_CPU_CYCLES_PER_SECOND = 2_300_000_000.0;
    public static final int POINT_TO_POINT_SEND_CLOCK_CYCLES = 75;
    public static final int SANDY_BRIDGE_CACHE_LATENCY_SEND = 107;
    public static final int SANDY_BRIDGE_INTERCONNECT_BANDWIDTH = 32; // bytes per cycle
    public static final int BROADCAST_CLOCK_CYCLES = POINT_TO_POINT_SEND_CLOCK_CYCLES;
    public static final int DEFAULT_BANDWIDTH_PER_CYCLE = 8; // one 64 bit register per clock cycle

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
     * All times are measured in <strong>seconds</strong>.
     * @param p2p_latency
     * @param broadcast_latency
     * @param p2p_bandwidth bytes per second
     * @param broadcast_bandwidth bytes per second
     */
    public MultiprocessorAttributes(double p2p_latency, double broadcast_latency,
                                    double p2p_bandwidth, double broadcast_bandwidth) {
        // latency
        this.broadcast_latency = broadcast_latency;
        this.p2p_latency = p2p_latency;
        // bandwidth
        this.p2p_bandwidth = p2p_bandwidth;
        this.broadcast_bandwidth = broadcast_bandwidth;
    }

    public MultiprocessorAttributes() {
        this(ACER_NITRO_CPU_CYCLES_PER_SECOND, POINT_TO_POINT_SEND_CLOCK_CYCLES, BROADCAST_CLOCK_CYCLES,
                DEFAULT_BANDWIDTH_PER_CYCLE, DEFAULT_BANDWIDTH_PER_CYCLE);
    }

    /**
     * Returns time in nanoseconds
     *
     * @param num_bytes
     * @param is_broadcast
     * @param no_latency
     * @return
     */
    public double getSendTime(int num_bytes, boolean is_broadcast, boolean no_latency) {
        // special case
        if (num_bytes == 0) {
            return 0.0;
        }
        if (is_broadcast) {
            double latency = no_latency ? 0 : this.broadcast_latency;
            return (latency + (num_bytes / this.broadcast_bandwidth)) * SEC_TO_NANO;
        } else {
            double latency = no_latency ? 0 : this.p2p_latency;
            return (latency + (num_bytes / this.p2p_bandwidth)) * SEC_TO_NANO;
        }
    }

    public double getSendTime(int num_words, boolean is_broadcast) {
        return getSendTime(num_words, is_broadcast, false);
    }


}
