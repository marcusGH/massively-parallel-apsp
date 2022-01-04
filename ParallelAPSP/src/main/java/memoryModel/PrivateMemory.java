package memoryModel;

import jdk.jshell.spi.ExecutionControl;
import util.Matrix;

import java.util.HashMap;
import java.util.Map;

public class PrivateMemory {
    // used in case we have a 1 x 1 private memory layout
    private Map<String, Double> singleMemoryDouble;
    // fast access to "A", "B", and "C" because used often
    // TODO: implement
    // used in case we have a 1 x n private memory layout
    // TODO: implement
    // used in case we have a n x 1 private memory layout
    // TODO: implement
    // used in the general case
    private Matrix<Map<String, Double>> matrixMemoryDouble;

    private final int k;

    public PrivateMemory(int k) {
        this.k = k;

        if (k == 1) {
            this.singleMemoryDouble = new HashMap<>();
        } else {
            this.matrixMemoryDouble = new Matrix<>(k, HashMap::new);
        }
    }

    public double getDouble(String label) {
        return this.getDouble(0, 0, label);
    }

    public void set(String label, Number n) {
        set(0, 0, label, n);
    }

    public void set(int mi, int  mj, String label, Number n) {
        if (n instanceof Double || n instanceof Integer) {
            set(mi, mj, label, n.doubleValue());
        } else {
            throw new RuntimeException("The type of Number n is not yet supported!");
        }
    }

    public void set(String label, double value) {
        this.set(0, 0, label, value);
    }


    public void set(int mi, int mj, String label, double value) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            this.singleMemoryDouble.put(label, value);
        } else {
            this.matrixMemoryDouble.get(mi, mj).put(label, value);
        }
    }

    public double getDouble(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            return this.singleMemoryDouble.get(label);
        } else {
            return this.matrixMemoryDouble.get(mi, mj).get(label);
        }
    }

    public int getInt(String label) {
        throw new RuntimeException(new ExecutionControl.NotImplementedException("This method is not yet implemented"));
    }

    public int getInt(int mi, int mj, String label) {
        throw new RuntimeException(new ExecutionControl.NotImplementedException("This method is not yet implemented"));
    }

    public void set(String label, int value) {
        throw new RuntimeException(new ExecutionControl.NotImplementedException("This method is not yet implemented"));
    }

    public void set(int mi, int mj, String label, int value) {
        throw new RuntimeException(new ExecutionControl.NotImplementedException("This method is not yet implemented"));
    }
}

