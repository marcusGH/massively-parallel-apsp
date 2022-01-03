package memoryModel;

import util.Matrix;

import java.util.HashMap;
import java.util.Map;

public class PrivateMemory<T> {
    // used in case we have a 1 x 1 private memory layout
    private Map<String, T> singleMemory;
    // used in case we have a 1 x n private memory layout
    // TODO: implement
    // used in case we have a n x 1 private memory layout
    // TODO: implement
    // used in the general case
    private Matrix<Map<String, T>> matrixMemory;

    private final int k;

    public PrivateMemory(int k) {
        this.k = k;

        if (k == 1) {
            this.singleMemory = new HashMap<>();
        } else {
            this.matrixMemory = new Matrix<>(k, HashMap::new);
        }
    }

    public T get(String label) {
        return this.get(0, 0, label);
    }

    public void set(String label, T value) {
        this.set(0, 0, label, value);
    }


    public void set(int mi, int mj, String label, T value) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            this.singleMemory.put(label, value);
        } else {
            this.matrixMemory.get(mi, mj).put(label, value);
        }
    }

    public T get(int mi, int mj, String label) {
        assert 0 <= mi && mi < this.k;
        assert 0 <= mj && mj < this.k;

        if (this.k == 1) {
            return this.singleMemory.get(label);
        } else {
            return this.matrixMemory.get(mi, mj).get(label);
        }
    }
}

