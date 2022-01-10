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
     * We store everything as Numbers, and we fetch the values, they are casted depending
     * on whether we want a double or an int. The values are auto-boxed upon setting.
     * @param mi
     * @param mj
     * @param label
     * @param n
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

    public double getDouble(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            if (!this.singleMemory.containsKey(label)) {
                throw new IllegalStateException(String.format("singleMemory does not contain label %s, "
                    + "only labels %s.", label, this.singleMemory.keySet()));
            }
            return this.singleMemory.get(label).doubleValue();
        } else {
            return this.matrixMemory.get(mi, mj).get(label).doubleValue();
        }
    }

    public int getInt(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            if (!this.singleMemory.containsKey(label)) {
                throw new IllegalStateException(String.format("singleMemory does not contain label %s, "
                        + "only labels %s.", label, this.singleMemory.keySet()));
            }
            return this.singleMemory.get(label).intValue();
        } else {
            return this.matrixMemory.get(mi, mj).get(label).intValue();
        }
    }
}

