package memoryModel;

import util.Matrix;

import java.util.HashMap;
import java.util.Map;

public class PrivateMemory {
    // used in case we have a 1 x 1 private memory layout
    private Map<String, Number> singleMemory;

    // fast access to "A", "B", and "C" because used often
    // TODO: implement
    // used in case we have a 1 x n private memory layout
    // TODO: implement
    // used in case we have a n x 1 private memory layout
    // TODO: implement

    // used in the general case
    private Matrix<Map<String, Number>> matrixMemory;

    private final int k;

    /**
     * Constructs a matrix of size k x k that acts as a private memory. When accessing the memory, a triplet
     * (mi, mj, label) is used, which uniquely identifies a Number at position (mi, mj) in the square matrix, labeled
     * with {@code label} to distinguish multiple numbers stored in the same location. Initializing the PrivateMemory
     * with k > 1 is suitable when a processing element should handle more than one cell of the input matrix.
     *
     * @param k size of the square matrix
     */
    public PrivateMemory(int k) {
        this.k = k;

        if (k == 1) {
            this.singleMemory = new HashMap<>();
        } else {
            this.matrixMemory = new Matrix<>(k, HashMap::new);
        }
    }

    // shorthands for the non-general cases

    public void set(String label, Number n) {
        set(0, 0, label, n);
    }

    public int getInt(String label) {
        return this.getInt(0, 0, label);
    }

    public double getDouble(String label) {
        return this.getDouble(0, 0, label);
    }


    /**
     * Every value is stored in a boxed Number, regardless of its type. When getters are used, the value is casted
     * according to the getter used, or inferred if the default getter is used.
     *
     * @param mi an integer ID
     * @param mj an integer ID
     * @param label a string label
     * @param n any instance of Number, subtype of it, or simply a primitive value like 3.13, in which case
     *          the value is automatically boxed.
     */
    public void set(int mi, int  mj, String label, Number n) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (!(n instanceof Double || n instanceof Integer)) {
            throw new RuntimeException("The type of Number n is not supported: " + n.getClass().getCanonicalName());
        }

        if (this.k == 1) {
            this.singleMemory.put(label, n);
        } else {
            this.matrixMemory.get(mi, mj).put(label, n);
        }

    }

    public Number get(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            if (!this.singleMemory.containsKey(label)) {
                throw new IllegalStateException(String.format("singleMemory does not contain label %s, "
                        + "only labels %s.", label, this.singleMemory.keySet()));
            }
            return this.singleMemory.get(label);
        } else {
            if (!this.matrixMemory.get(mi, mj).containsKey(label)) {
                throw new IllegalStateException(String.format("matrixMemory does not contain label %s at (%d, %d), "
                        + "only labels %s.", label, mi, mj, this.singleMemory.keySet()));
            }
            return this.matrixMemory.get(mi, mj).get(label);
        }
    }

    public double getDouble(int mi, int mj, String label) {
        return this.get(mi, mj, label).doubleValue();
    }

    public int getInt(int mi, int mj, String label) {
        return this.get(mi, mj, label).intValue();
    }
}

