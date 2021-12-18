package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class MatrixTest {

    @Test
    @DisplayName("Correct value after initialisation (test 1)")
    void get1() {
        Matrix<Integer> m  = new Matrix<>(5, () -> 42);
        assertEquals(42, m.get(2, 3));
    }

    @Test
    @DisplayName("Correct value after initialisation (test 2)")
    void get2() {
        Matrix<Double> m = new Matrix<>(2, () -> 3.14);
        assertEquals(3.14, m.get(0, 0));
        assertEquals(3.14, m.get(1, 0));
        assertEquals(3.14, m.get(0, 1));
        assertEquals(3.14, m.get(1, 1));
    }

    @Test
    @DisplayName("Set only modifies one value")
    void set() {
        Matrix<Integer> m = new Matrix<>(2, () -> 10);
        m.set(0, 1, 42);
        assertEquals(10, m.get(0, 0));
        assertEquals(10, m.get(1, 0));
        assertEquals(42, m.get(0, 1));
        assertEquals(10, m.get(1, 1));
    }

    @Test
    @DisplayName("Objects are unique")
    void uniqueQueues() {
        Matrix<Queue<Integer>> m = new Matrix<Queue<Integer>>(2, LinkedList::new);
        m.get(0, 1).add(42);
        // there should now only be 3 empty queues
        assertEquals(0, m.get(0,0).size());
        assertEquals(1, m.get(0,1).size());
        assertEquals(0, m.get(1,0).size());
        assertEquals(0, m.get(1,1).size());
    }
}