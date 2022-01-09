package memoryModel;

import jdk.jshell.spi.ExecutionControl;
import util.Matrix;

import java.util.HashMap;
import java.util.Map;

public class PrivateMemory {
    // used in case we have a 1 x 1 private memory layout
    private Map<String, Double> singleMemoryDouble;
    private Map<String, Integer> singleMemoryInteger;
    // fast access to "A", "B", and "C" because used often
    // TODO: implement
    // used in case we have a 1 x n private memory layout
    // TODO: implement
    // used in case we have a n x 1 private memory layout
    // TODO: implement
    // used in the general case
    private Matrix<Map<String, Double>> matrixMemoryDouble;
    private Matrix<Map<String, Integer>> matrixMemoryInteger;

    private final int k;

    public PrivateMemory(int k) {
        this.k = k;

        if (k == 1) {
            this.singleMemoryDouble = new HashMap<>();
            this.singleMemoryInteger = new HashMap<>();
        } else {
            this.matrixMemoryDouble = new Matrix<>(k, HashMap::new);
            this.matrixMemoryInteger = new Matrix<>(k, HashMap::new);
        }
    }

    // shorthands for the non-general cases

    public void set(String label, Number n) {
        set(0, 0, label, n);
    }

    public void set(String label, int n) {
        set(0, 0, label, n);
    }

    public void set(String label, double n) {
        this.set(0, 0, label, n);
    }

    public void setInt(String label, int n) {
        this.setInt(0, 0, label, n);
    }

    public void setDouble(String label, double n) {
        this.setDouble(0, 0, label, n);
    }

    public int getInt(String label) {
        return this.getInt(0, 0, label);
    }

    public double getDouble(String label) {
        return this.getDouble(0, 0, label);
    }


    /**
     * If no type is specified, it will be inferred
     * @param mi
     * @param mj
     * @param label
     * @param n
     */
    public void set(int mi, int  mj, String label, Number n) {
        if (n instanceof Double) {
            setDouble(mi, mj, label, n.doubleValue());
        } else if (n instanceof Integer) {
            setInt(mi, mj, label, n.intValue());
        } else {
            throw new RuntimeException("The type of Number n is not yet supported!");
        }
    }

    public void setDouble(int mi, int mj, String label, double n) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            this.singleMemoryDouble.put(label, n);
        } else {
            this.matrixMemoryDouble.get(mi, mj).put(label, n);
        }
    }

    public void setInt(int mi, int mj, String label, int n) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            this.singleMemoryInteger.put(label, n);
        } else {
            this.matrixMemoryInteger.get(mi, mj).put(label, n);
        }
    }

    public double getDouble(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            if (!this.singleMemoryDouble.containsKey(label)) {
                throw new IllegalStateException(String.format("singleMemoryDouble does not contain label %s, "
                    + "only labels %s. Additionally, singleMemoryInteger has elements %s.",
                        label, this.singleMemoryDouble.keySet(), this.singleMemoryInteger.keySet()));
            }
            return this.singleMemoryDouble.get(label);
        } else {
            return this.matrixMemoryDouble.get(mi, mj).get(label);
        }
    }

    public int getInt(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            if (!this.singleMemoryInteger.containsKey(label)) {
                throw new IllegalStateException(String.format("singleMemoryInteger does not contain label %s, "
                                + "only labels %s. Additionally, singleMemoryDouble has elements %s.",
                        label, this.singleMemoryInteger.keySet(), this.singleMemoryDouble.keySet()));
            }
            return this.singleMemoryInteger.get(label);
        } else {
            return this.matrixMemoryInteger.get(mi, mj).get(label);
        }
    }
}

