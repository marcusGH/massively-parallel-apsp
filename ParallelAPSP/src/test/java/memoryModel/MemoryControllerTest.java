package memoryModel;

import org.junit.jupiter.api.Test;
import util.Matrix;

import static org.junit.jupiter.api.Assertions.*;

class MemoryControllerTest {

    @Test
    void pointToPoint1() {
        // TODO: look up the 3 stages of unit tests

        Matrix<PrivateMemory<Integer>> privateMemory = new Matrix<>(3, () -> new PrivateMemory<>(1));
        Topology sgt = new SquareGridTopology(3);
        MemoryController<Integer> mc = new MemoryController<>(3, privateMemory, sgt);

        try {
            mc.sendData(0, 1, 1, 0, 42);
            mc.receiveData(1, 0, 0, 0, "A");
            mc.flush();
        } catch (CommunicationChannelCongestionException | InconsistentMemoryChannelUsageException e) {
            e.printStackTrace();
        }

        assertEquals(privateMemory.get(1, 0).get("A"), 42);
    }
}