package memoryModel;

import javafx.util.Pair;
import util.Matrix;
import util.Triple;

import javax.management.MBeanAttributeInfo;
import java.util.*;

public class MemoryController<T> {
    private int p;
    private Matrix<PrivateMemory<T>> privateMemories;
    private Topology memoryTopology;

    // broadcasting
    private List<Queue<T>> colBroadcastData;
    private List<Queue<T>> rowBroadcastData;
    // IDs of the PEs using the row- and column broadcast highways
    private List<Optional<Pair<Integer, Integer>>> colBroadcasterID;
    private List<Optional<Pair<Integer, Integer>>> rowBroadcasterID;
    private final Matrix<Queue<Triple<Integer, Integer, String>>> rowBroadcastReceiveArguments;
    private final Matrix<Queue<Triple<Integer, Integer, String>>> colBroadcastReceiveArguments;

    // point-to-point communications
    private Matrix<Queue<T>> sentData;
    // item (i, j) gives ID of the sender of the data destined to PE(i, j)
    private Matrix<Optional<Pair<Integer, Integer>>> senderToRecipientID;
    private Matrix<Queue<Triple<Integer, Integer, String>>> receiveArguments;

    public MemoryController(int p, Matrix<PrivateMemory<T>> privateMemories, Topology memoryTopology) {
        // TODO: check sizes of topology and see if match p
        this.p = p;
        this.privateMemories = privateMemories;
        this.memoryTopology = memoryTopology;

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

    synchronized void broadcastRow(int i, int j, T value) throws CommunicationChannelCongestionException {
        int row = i;
        Pair<Integer, Integer> newID = new Pair<>(i, j);
        if (this.rowBroadcasterID.get(row).isPresent() && !this.rowBroadcasterID.get(row).get().equals(newID)) {
            throw new CommunicationChannelCongestionException("The row broadcast highway with id "
                    + row + " is already in use by PE " + rowBroadcasterID.get(row)
                    + ", so PE (" + i + ", " + j + ") cannot use it.");
        } else {
            this.rowBroadcastData.get(row).add(value);
            this.rowBroadcasterID.set(row, Optional.of(newID));
        }
    }

    synchronized void broadcastCol(int i, int j, T value) throws CommunicationChannelCongestionException {
        int col = j;
        Pair<Integer, Integer> newID = new Pair<>(i, j);
        Optional<Pair<Integer, Integer>> oldID = this.colBroadcasterID.get(col);
        if (oldID.isPresent() && !oldID.get().equals(newID)) {
            // TODO: refactor to be consistent with below style
            // TODO: refactor to use same fix above
            throw new CommunicationChannelCongestionException("The column broadcast highway with id "
                    + col + " is already in use by PE " + oldID
                    + ", so PE (" + i + ", " + j + ") cannot use it.");
        } else {
            this.colBroadcastData.get(col).add(value);
            this.colBroadcasterID.set(col, Optional.of(newID));
        }
    }

    void receiveRowBroadcast(int i, int j, int mi, int mj, String label) {
        synchronized (this.rowBroadcastReceiveArguments) {
            this.rowBroadcastReceiveArguments.get(i, j).add(new Triple<>(mi, mj, label));
        }
    }

    void receiveColBroadcast(int i, int j, int mi, int mj, String label) {
        synchronized (this.colBroadcastReceiveArguments) {
            this.colBroadcastReceiveArguments.get(i, j).add(new Triple<>(mi, mj, label));
        }
    }

    synchronized void sendData(int sendI, int sendJ, int receiveI, int receiveJ, T value) throws CommunicationChannelCongestionException {
        // TODO: count number of hops before doing below functionality

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

    synchronized void receiveData(int i, int j, String label) {
        this.receiveData(i, j, 0, 0, label);
    }

    synchronized void receiveData(int i, int j, int mi, int mj, String label) {
        this.receiveArguments.get(i, j).add(new Triple<>(mi, mj, label));
    }

    // doesn't need to be synchronised, but just in  case to demonstrate not run at same time
    // as above methods
    synchronized public void flush() throws InconsistentMemoryChannelUsageException {
        // we handle the point-to-point communication first
        for (int i = 0; i < this.p; i++) {
            for (int j = 0; j < this.p; j++) {
                Queue<T> sentDataQueue = this.sentData.get(i, j);
                Queue<Triple<Integer, Integer, String>> receiveArgumentsQueue = this.receiveArguments.get(i, j);

                // match up sent data with receive arguments
                while (!sentDataQueue.isEmpty() && !receiveArgumentsQueue.isEmpty()) {
                    T datum = sentDataQueue.poll();
                    Triple<Integer, Integer, String> args = receiveArgumentsQueue.poll();
                    // we checked if empty in while loop above
                    assert args != null;

                    this.privateMemories.get(i, j).set(args.getFirst(), args.getSecond(), args.getThird(), datum);
                }

                // both queues should be empty now
                if (!sentDataQueue.isEmpty() || !receiveArgumentsQueue.isEmpty()) {
                    throw new InconsistentMemoryChannelUsageException("Processing element PE(" + i + ", " + j
                            + ") did not receive as many data items as it specified it would receive");
                }
            }
        }

        // we now handle row-broadcasting
        for (int i = 0; i < this.p; i++) {
            Queue<T> rowBroadcastDataQueue = this.rowBroadcastData.get(i);
            while (!rowBroadcastDataQueue.isEmpty()) {
                T value = rowBroadcastDataQueue.poll();
                for (int j = 0; j < this.p; j++) {
                    Queue<Triple<Integer, Integer, String>> rowReceiveArgumentsQueue = this.rowBroadcastReceiveArguments.get(i, j);
                    Triple<Integer, Integer, String> args = rowReceiveArgumentsQueue.poll();
                    // nothing to do
                    if (value == null && args == null) continue;
                    // look for inconsistencies
                    if (value == null || args == null) {
                        throw new InconsistentMemoryChannelUsageException("Processing element PE(" + i + ", " + j
                                + ") did not receive as many data items as it specified it would receive through row broadcast");
                    } else {
                        this.privateMemories.get(i, j).set(args.getFirst(), args.getSecond(), args.getThird(), value);
                    }
                }
            }
        }

        // we then handle column-broadcasting
        for (int j = 0; j < this.p; j++) {
            Queue<T> colBroadcastDataQueue = this.colBroadcastData.get(j);
            while (!colBroadcastDataQueue.isEmpty()) {
                T value = colBroadcastDataQueue.poll();
                for (int i = 0; i < this.p; i++) {
                    Queue<Triple<Integer, Integer, String>> colReceiveArgumentsQueue = this.colBroadcastReceiveArguments.get(i, j);
                    Triple<Integer, Integer, String> args = colReceiveArgumentsQueue.poll();
                    // nothing to do
                    if (value == null && args == null) continue;
                    // look for inconsistencies
                    if (value == null || args == null) {
                        throw new InconsistentMemoryChannelUsageException("Processing element PE(" + i + ", " + j
                                + ") did not receive as many data items as it specified it would receive through column broadcast");
                    } else {
                        this.privateMemories.get(i, j).set(args.getFirst(), args.getSecond(), args.getThird(), value);
                    }
                }
            }
        }
    }
}
