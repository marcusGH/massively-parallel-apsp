package memoryModel;

import org.junit.jupiter.api.Test;
import util.Matrix;

import static org.junit.jupiter.api.Assertions.*;

class MemoryControllerTest {

    @Test
    void pointToPoint1() {
        // SETUP
        Matrix<PrivateMemory<Integer>> privateMemory = new Matrix<>(3, () -> new PrivateMemory<>(1));
        Topology sgt = new SquareGridTopology(3);
        MemoryController<Integer> mc = new MemoryController<>(3, privateMemory, sgt);

        // ACT
        try {
            mc.sendData(0, 1, 1, 0, 42);
            mc.receiveData(1, 0, 0, 0, "A");
            mc.flush();
        } catch (CommunicationChannelCongestionException | InconsistentMemoryChannelUsageException e) {
            e.printStackTrace();
        }

        // ASSERT
        assertEquals(privateMemory.get(1, 0).get("A"), 42);
    }

    @Test
    void pointToPoint2() {
        // SETUP

        // 5 x 5 grid of PEs each with 1 x 1 private memory
        Matrix<PrivateMemory<Double>> privateMemory = new Matrix<>(5, () -> new PrivateMemory<>(1));
        Topology sgt = new SquareGridTopology(5);
        MemoryController<Double> mc = new MemoryController<>(5, privateMemory, sgt);

        // ACT
        try {
            // send data 3.14 from (2, 1) to (3, 3)
            mc.sendData(2, 1, 3, 3, 3.14);
            mc.receiveData(3, 3, "A");
            // send data 2.71 from (4, 3) to (2, 1)
            mc.sendData(4, 3, 2, 1, 2.71);
            mc.receiveData(2, 1, "A");
            mc.flush();
        } catch (CommunicationChannelCongestionException | InconsistentMemoryChannelUsageException e) {
            e.printStackTrace();
        }

        // ASSERT
        assertEquals(privateMemory.get(3, 3).get("A"), 3.14);
        assertEquals(privateMemory.get(2, 1).get("A"), 2.71);
    }

    @Test
    void broadcastRow1() {
        // SETUP
        Matrix<PrivateMemory<Double>> privateMemory = new Matrix<>(3, () -> new PrivateMemory<>(1));
        Topology sgt = new SquareGridTopology(3);
        MemoryController<Double> mc = new MemoryController<>(3, privateMemory, sgt);

        // ACT
        try {
            // PE(2,1) broadcasts a row
            mc.broadcastRow(2, 1, 3.14);
            for (int j = 0; j < 3; j++) {
                mc.receiveRowBroadcast(2, j, 0, 0, "A");
            }
            mc.flush();
        } catch (CommunicationChannelCongestionException | InconsistentMemoryChannelUsageException e) {
            e.printStackTrace();
        }

        // ASSERT
        assertEquals(privateMemory.get(2, 0).get("A"), 3.14);
        assertEquals(privateMemory.get(2, 1).get("A"), 3.14);
        assertEquals(privateMemory.get(2, 2).get("A"), 3.14);
    }
}