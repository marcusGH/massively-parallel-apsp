package memoryModel;

import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import util.Matrix;

import static org.junit.jupiter.api.Assertions.*;

class MemoryControllerTest {

    @Test
    @Description("Tests point-to-point communication with sendData and receiveData")
    void pointToPoint1() {
        // SETUP
        Matrix<PrivateMemory> privateMemory = new Matrix<>(3, () -> new PrivateMemory(1));
        MemoryController mc = new MemoryController(3, privateMemory, SquareGridTopology::new);

        // ACT
        try {
            mc.sendData(0, 1, 1, 0, 42);
            mc.receiveData(1, 0, 0, 0, "A");
            mc.flush();
        } catch (CommunicationChannelCongestionException | InconsistentMemoryChannelUsageException e) {
            e.printStackTrace();
        }

        // ASSERT
        assertEquals(privateMemory.get(1, 0).getDouble("A"), 42);
    }

    @Test
    @Description("Tests point-to-point communication with sendData and receiveData")
    void pointToPoint2() {
        // SETUP

        // 5 x 5 grid of PEs each with 1 x 1 private memory
        Matrix<PrivateMemory> privateMemory = new Matrix<>(5, () -> new PrivateMemory(1));
        MemoryController mc = new MemoryController(5, privateMemory, SquareGridTopology::new);

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
        assertEquals(privateMemory.get(3, 3).getDouble("A"), 3.14);
        assertEquals(privateMemory.get(2, 1).getDouble("A"), 2.71);
    }

    @Test
    @Description("Tests row broadcast communication with broadcastRow and receiveRowBroadcast")
    void broadcastRow1() {
        // SETUP
        Matrix<PrivateMemory> privateMemory = new Matrix<>(3, () -> new PrivateMemory(1));
        MemoryController mc = new MemoryController(3, privateMemory, SquareGridTopology::new);

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
        assertEquals(privateMemory.get(2, 0).getDouble("A"), 3.14);
        assertEquals(privateMemory.get(2, 1).getDouble("A"), 3.14);
        assertEquals(privateMemory.get(2, 2).getDouble("A"), 3.14);
    }


    @Test
    @Description("Tests col broadcast communication with broadcastCol and receiveColBroadcast")
    void broadcastCol1() {
        // SETUP
        Matrix<PrivateMemory> privateMemory = new Matrix<>(3, () -> new PrivateMemory(1));
        MemoryController mc = new MemoryController(3, privateMemory, SquareGridTopology::new);

        // ACT
        try {
            // PE(2,1) broadcasts a column
            mc.broadcastCol(2, 1, 3.14);
            for (int i = 0; i < 3; i++) {
                mc.receiveColBroadcast(i, 1, 0, 0, "A");
            }
            mc.flush();
        } catch (InconsistentMemoryChannelUsageException | CommunicationChannelCongestionException e) {
            e.printStackTrace();
        }

        // ASSERT
        assertEquals(privateMemory.get(0, 1).getDouble("A"), 3.14);
        assertEquals(privateMemory.get(1, 1).getDouble("A"), 3.14);
        assertEquals(privateMemory.get(2, 1).getDouble("A"), 3.14);
    }

    // TODO: add more tests for when things go wrong, and assert correct exception is thrown
}