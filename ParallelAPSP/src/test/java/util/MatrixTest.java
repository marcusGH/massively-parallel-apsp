package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixTest {

    @Test
    @DisplayName("Correct value after initialisation (test 1)")
    void get1() {
        Matrix<Integer> m  = new Matrix<>(5, 42);
        assertEquals(42, m.get(2, 3));
    }

    @Test
    @DisplayName("Correct value after initialisation (test 2)")
    void get2() {
        Matrix<Double> m = new Matrix<>(2, 3.14);
        assertEquals(3.14, m.get(0, 0));
        assertEquals(3.14, m.get(1, 0));
        assertEquals(3.14, m.get(0, 1));
        assertEquals(3.14, m.get(1, 1));
    }

    @Test
    @DisplayName("Set only modifies one value")
    void set() {
        Matrix<Integer> m = new Matrix<>(2, 10);
        m.set(0, 1, 42);
        assertEquals(10, m.get(0, 0));
        assertEquals(10, m.get(1, 0));
        assertEquals(42, m.get(0, 1));
        assertEquals(10, m.get(1, 1));
    }
}