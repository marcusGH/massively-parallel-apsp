package memoryModel;

import javafx.util.Pair;
import jdk.jshell.spi.ExecutionControl;
import memoryModel.topology.SquareGridTopology;
import memoryModel.topology.Topology;
import util.Matrix;
import util.Triple;

import java.util.*;
import java.util.function.Function;

// TODO: switch from synchronised to thread-safe queues etc.

/**
 * <p>A MemoryController instance can be used as a "fine-grained monitor" for simulating the point-to-point and
 * broadcasting communication happening in a multiprocessor where the processing elements are arranged in a square
 * grid and interconnected through some topology specified in the constructor. Only one instance of this class should
 * be made per simulated multiprocessor, and since this class' method are all thread-safe, all the processing elements
 * can invoke methods on the MemoryController object concurrently in a safe manner.</p>
 *
 * <p>The relevant point-to-point communication methods are:
 * <ul>
 *     <li> sendData </li>
 *     <li> receiveData </li>
 * </ul>
 * The relevant broadcast communication methods are:
 * <ul>
 *     <li> broadcastRow </li>
 *     <li> broadcastCol </li>
 *     <li> receiveRowBroadcast </li>
 *     <li> receiveColBroadcast </li>
 * </ul>
 * When calling any of the receive methods, a triplet "receive-argument" on the form (mi, mj, label) must be specified.
 * These three values are used when accessing the setter method of the PrivateMemory that should receive the number
 * {@link PrivateMemory#set(int, int, String, Number)}.
 * </p>
 *
 * <p> When any of the above methods are called, no changes will be made to the {@code privateMemories} supplied in the
 * constructor. The values to be sent and the receive-arguments are just stored in a queue and the actual changes to
 * {@code privateMemories} happen when {@link #flush()} is called. Additionally, all communication must happen in two
 * parts: The sender must tell the memoryController that it wants to send something, and the receiver must tell the
 * memoryController that it wants to receive some data, and how this received data should be stored.
 * </p>
 */
public class MemoryController {
    private int p;
    private Matrix<PrivateMemory> privateMemories;
    // TODO: refactor this out of the constructor and make new class: CountingMemoryController,
    //       which wraps this one and implements functionality to count memory transfers, their cost
    //       and can return a list of these ...
    private Topology memoryTopology;

    // broadcasting
    private final List<Queue<Number>> colBroadcastData;
    private final List<Queue<Number>> rowBroadcastData;
    // IDs of the PEs using the row- and column broadcast highways
    private final List<Optional<Pair<Integer, Integer>>> colBroadcasterID;
    private final List<Optional<Pair<Integer, Integer>>> rowBroadcasterID;
    private final Matrix<Queue<Triple<Integer, Integer, String>>> rowBroadcastReceiveArguments;
    private final Matrix<Queue<Triple<Integer, Integer, String>>> colBroadcastReceiveArguments;

    // point-to-point communications
    private final Matrix<Queue<Number>> sentData;
    // item (i, j) gives ID of the sender of the data destined to PE(i, j)
    private final Matrix<Optional<Pair<Integer, Integer>>> senderToRecipientID;
    private final Matrix<Queue<Triple<Integer, Integer, String>>> receiveArguments;

    public int getProcessingElementGridSize() {
        return p;
    }

    public Matrix<PrivateMemory> getPrivateMemories() {
        return privateMemories;
    }

    /**
     * This constructor does not make a deep-copy of the provided object, but rather reuses its private memories.
     * @param memoryController
     */
    public MemoryController(MemoryController memoryController) {
        this(memoryController.getProcessingElementGridSize(), memoryController.getPrivateMemories(), SquareGridTopology::new);
    }

    /**
     * Constructs a MemoryController handling p x p processing elements, each starting with the private memory contents
     * as contained in the passed privateMemories. A constructor for the memory topology must be provided. Note that
     * the private memories of each processing elements do not need to be of size 1 x 1.
     *
     * @param p a positive integer
     * @param privateMemories a matrix of private memories of type T
     * @param memoryTopologySupplier a constructor taking an Integer and returning a Topology
     */
    public MemoryController(int p, Matrix<PrivateMemory> privateMemories, Function<Integer, ? extends Topology> memoryTopologySupplier) {
        this.p = p;
        this.privateMemories = privateMemories;
        this.memoryTopology = memoryTopologySupplier.apply(p);

        // broadcasting
        // we will only have p elements at all times
        this.colBroadcastData = new ArrayList<>(p);
        this.rowBroadcastData = new ArrayList<>(p);
        this.colBroadcasterID = new ArrayList<>(p);
        this.rowBroadcasterID = new ArrayList<>(p);
        for (int i = 0; i < p; i++) {
            this.colBroadcastData.add(new LinkedList<>());
            this.rowBroadcastData.add(new LinkedList<>());
            this.colBroadcasterID.add(Optional.empty());
            this.rowBroadcasterID.add(Optional.empty());
        }
        this.rowBroadcastReceiveArguments = new Matrix<>(p, LinkedList::new);
        this.colBroadcastReceiveArguments = new Matrix<>(p, LinkedList::new);

        // point-to-point
        this.sentData = new Matrix<>(p, LinkedList::new);
        this.senderToRecipientID = new Matrix<>(p, Optional::empty);
        this.receiveArguments = new Matrix<>(p, LinkedList::new);
    }

    /**
     * When {@link #flush()} is invoked, all processing elements in row i will receive data {@code value},
     * but only if they themself invoke {@link #receiveRowBroadcast}. If another processing element in row i
     * that is not (i, j) tries to row broadcast in the same communication phase, an exception is thrown.
     *
     * @param i non-negative integer index
     * @param j non-negative integer index
     * @param value the value to be broadcasted
     * @throws CommunicationChannelCongestionException if it's not possible to perform all the communications
     *         scheduled in parallel without queueing.
     */
    public void broadcastRow(int i, int j, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.rowBroadcastData) {
            synchronized (this.rowBroadcasterID) {
                Pair<Integer, Integer> newID = new Pair<>(i, j);
                Optional<Pair<Integer, Integer>> oldID = this.rowBroadcasterID.get(i);
                if (oldID.isPresent() && !oldID.get().equals(newID)) {
                    throw new CommunicationChannelCongestionException(String.format("The row broadcast highway with id "
                            + "%d is already in use by PE(%d, %d), so PE(%d, %d) cannot use it.",
                            i, oldID.get().getKey(), oldID.get().getValue(), i, j));
                } else {
                    this.rowBroadcastData.get(i).add(value);
                    this.rowBroadcasterID.set(i, Optional.of(newID));
                }
            }
        }
    }

    /**
     * When {@link #flush()} is invoked, all processing elements in column j will receive data {@code value},
     * but only if they themself invoke {@link #receiveColBroadcast}. If another processing element in column j
     * that is not (i, j) tries to column broadcast in the same communication phase, an exception is thrown.
     *
     * @param i non-negative integer index
     * @param j non-negative integer index
     * @param value the value to be broadcasted
     * @throws CommunicationChannelCongestionException if it's not possible to perform all the communications
     *         scheduled in parallel without queueing.
     */
    public void broadcastCol(int i, int j, Number value) throws CommunicationChannelCongestionException {
        synchronized (this.colBroadcastData) {
            synchronized (this.colBroadcasterID) {
                Pair<Integer, Integer> newID = new Pair<>(i, j);
                Optional<Pair<Integer, Integer>> oldID = this.colBroadcasterID.get(j);
                if (oldID.isPresent() && !oldID.get().equals(newID)) {
                    // TODO: refactor to be consistent with below style
                    throw new CommunicationChannelCongestionException(String.format("The column broadcast highway with id "
                                    + "%d is already in use by PE(%d, %d), so PE(%d, %d) cannot use it.",
                            j, oldID.get().getKey(), oldID.get().getValue(), i, j));
                } else {
                    this.colBroadcastData.get(j).add(value);
                    this.colBroadcasterID.set(j, Optional.of(newID));
                }
            }
        }
    }

    /**
     * Tells the memory controller that processing element (i, j) wants to receive data from some other processing
     * element, or from itself, located in row i. The received data should be stored in its private memory by calling
     * PrivateMemory::set with arguments (mi, mj, label).
     *
     * @param i non-negative integer smaller than p
     * @param j non-negative integer smaller than p
     * @param mi non-negative integer smaller than the private memory size
     * @param mj non-negative integer smaller than the private memory size
     * @param label String label used in private memory access
     */
    public void receiveRowBroadcast(int i, int j, int mi, int mj, String label) {
        synchronized (this.rowBroadcastReceiveArguments) {
            this.rowBroadcastReceiveArguments.get(i, j).add(new Triple<>(mi, mj, label));
        }
    }

    /**
     * Tells the memory controller that processing element (i, j) wants to receive data from some other processing
     * element, or from itself, located in column j. The received data should be stored in its private memory by calling
     * PrivateMemory::set with arguments (mi, mj, label).
     *
     * @param i non-negative integer smaller than p
     * @param j non-negative integer smaller than p
     * @param mi non-negative integer smaller than the private memory size
     * @param mj non-negative integer smaller than the private memory size
     * @param label String label used in private memory access
     */
    public void receiveColBroadcast(int i, int j, int mi, int mj, String label) {
        synchronized (this.colBroadcastReceiveArguments) {
            this.colBroadcastReceiveArguments.get(i, j).add(new Triple<>(mi, mj, label));
        }
    }

    /**
     * Tells the memory controller that processing element (sendI, sendJ) sends data {@code value} to processing
     * element (receiveI, receiveJ) through point-to-point communication. The processing element (receiveI, receiveJ)
     * should then invoke {@link #receiveData} to tell the memory controller how it wants to store the
     * received data. If different processing elements send data at the same time to the same node, an exception is
     * thrown. This is because if all nodes symmetrically ran the same code, we would have a congested communication
     * channel, so it would not be possible to complete the required communication in one phase.
     *
     * @param sendI non-negative integer ID less than p
     * @param sendJ non-negative integer ID less than p
     * @param receiveI non-negative integer ID less than p
     * @param receiveJ non-negative integer ID less than p
     * @param value the data to be sent
     * @throws CommunicationChannelCongestionException if different processing elements tries to send data to the same
     * node in the same communication phase.
     */
    public void sendData(int sendI, int sendJ, int receiveI, int receiveJ, Number value) throws CommunicationChannelCongestionException {
        // TODO: do input sanitation here and throw check exception in case of failure. Otherwise, programmer errors
        //       will lead to program not halting because one or more threads terminate on unchecked exception and
        //       don't reach the cyclic barrier! (See manager test one if you do send to index out of bounds)

        synchronized (this.sentData) {
            synchronized (this.senderToRecipientID) {
                Pair<Integer, Integer> newID = new Pair<>(sendI, sendJ);
                Optional<Pair<Integer, Integer>> oldID = this.senderToRecipientID.get(receiveI, receiveJ);
                // We are trying to send data to same recipient from multiple PEs, which would cause nondeterministic behaviour
                if (oldID.isPresent() && !oldID.get().equals(newID)) {
                    throw new CommunicationChannelCongestionException(String.format("The recipient PE(%d, %d) is already "
                                    + "receiving data from PE(%d, %d), so it can't receive data from PE(%d, %d).",
                            receiveI, receiveJ, oldID.get().getKey(), oldID.get().getValue(), sendI, sendJ));
                }
                // New sender or same sender that sent data previously to this PE
                else {
                    this.sentData.get(receiveI, receiveJ).add(value);
                    this.senderToRecipientID.set(receiveI, receiveJ, Optional.of(newID));
                }
            }
        }
    }

    public void receiveData(int i, int j, String label) {
        this.receiveData(i, j, 0, 0, label);
    }

    /**
     * Tells the memory controller than processing element (i, j) wants to receive point-to-point communicated data
     * and store it in its private memory by invoking PrivateMemory::set with arguments (mi, mj, label).
     *
     * @param i non-negative integer ID less than p
     * @param j non-negative integer ID less than p
     * @param mi non-negative integer ID to private memory
     * @param mj non-negative integer ID to private memory
     * @param label String label indicating which memory to store it in
     */
    public void receiveData(int i, int j, int mi, int mj, String label) {
        synchronized (this.receiveArguments) {
            this.receiveArguments.get(i, j).add(new Triple<>(mi, mj, label));
        }
    }

    // doesn't need to be synchronised, but just in  case to demonstrate not run at same time
    // as above methods

    /**
     * When flush is invoked, the memory controller will attempt to align all the scheduled row broadcasting,
     * column broadcasting, and point-to-point data sent, with the corresponding specified receive-arguments. If a
     * mismatch is found, an exception is thrown. Otherwise, the memory controller will go through the pairs of data
     * and receive-arguments and invoke PrivateMemory::set on the processing elements with the corresponding data and
     * receive-arguments. When flush has finished, the {@code privateMemories} passed in the constructor will have been
     * modified, according to all the sendData, broadcastRow/Col, receiveData and receiveRow/ColBroadcast methods that
     * have been executed since the last flush.
     *
     * @throws InconsistentCommunicationChannelUsageException if one processing element is scheduled to receive more data than
     * it has provided receive-arguments more, or vice verse.
     */
    synchronized public void flush() throws InconsistentCommunicationChannelUsageException {
        // we handle the point-to-point communication first
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Queue<Number> sentDataQueue = this.sentData.get(i, j);
                Queue<Triple<Integer, Integer, String>> receiveArgumentsQueue = this.receiveArguments.get(i, j);

                // match up sent data with receive arguments
                while (!sentDataQueue.isEmpty() && !receiveArgumentsQueue.isEmpty()) {
                    Number datum = sentDataQueue.poll();
                    Triple<Integer, Integer, String> args = receiveArgumentsQueue.poll();
                    // we checked if empty in while loop above
                    assert args != null;

                    this.privateMemories.get(i, j).set(args.getFirst(), args.getSecond(), args.getThird(), datum);
                }

                // both queues should be empty now
                if (!sentDataQueue.isEmpty() || !receiveArgumentsQueue.isEmpty()) {
                    throw new InconsistentCommunicationChannelUsageException("Processing element PE(" + i + ", " + j
                            + ") did not receive as many data items as it specified it would receive");
                }
            }
        }

        // we now handle row-broadcasting
        for (int i = 0; i < this.p; i++) {
            Queue<Number> rowBroadcastDataQueue = this.rowBroadcastData.get(i);;
            while (!rowBroadcastDataQueue.isEmpty()) {
                Number value = rowBroadcastDataQueue.poll();
                for (int j = 0; j < this.p; j++) {
                    Queue<Triple<Integer, Integer, String>> rowReceiveArgumentsQueue = this.rowBroadcastReceiveArguments.get(i, j);
                    Triple<Integer, Integer, String> args = rowReceiveArgumentsQueue.poll();
                    // nothing to do
                    if (value == null && args == null) continue;
                    // look for inconsistencies
                    if (value == null || args == null) {
                        throw new InconsistentCommunicationChannelUsageException("Processing element PE(" + i + ", " + j
                                + ") did not receive as many data items as it specified it would receive through row broadcast");
                    } else {
                        this.privateMemories.get(i, j).set(args.getFirst(), args.getSecond(), args.getThird(), value);
                    }
                }
            }
        }

        // we then handle column-broadcasting
        for (int j = 0; j < this.p; j++) {
            Queue<Number> colBroadcastDataQueue = this.colBroadcastData.get(j);
            while (!colBroadcastDataQueue.isEmpty()) {
                Number value = colBroadcastDataQueue.poll();
                for (int i = 0; i < this.p; i++) {
                    Queue<Triple<Integer, Integer, String>> colReceiveArgumentsQueue = this.colBroadcastReceiveArguments.get(i, j);
                    Triple<Integer, Integer, String> args = colReceiveArgumentsQueue.poll();
                    // nothing to do
                    if (value == null && args == null) continue;
                    // look for inconsistencies
                    if (value == null || args == null) {
                        throw new InconsistentCommunicationChannelUsageException("Processing element PE(" + i + ", " + j
                                + ") did not receive as many data items as it specified it would receive through column broadcast");
                    } else {
                        this.privateMemories.get(i, j).set(args.getFirst(), args.getSecond(), args.getThird(), value);
                    }
                }
            }
        }

        // we then reset all the sender IDs
        this.senderToRecipientID.setAll(Optional::empty);
        for (int i = 0; i < this.p; i++) {
            this.rowBroadcasterID.set(i, Optional.empty());
            this.colBroadcasterID.set(i, Optional.empty());
        }
    }

    // TODO: implement
    public void getPointToPointCommunicationCounts() throws ExecutionControl.NotImplementedException {
        throw new ExecutionControl.NotImplementedException("This method is not yet implemented");
    }

    // TODO: implement
    public void getBroadcastCommunicationCounts() throws ExecutionControl.NotImplementedException {
        throw new ExecutionControl.NotImplementedException("This method is not yet implemented");
    }

}
