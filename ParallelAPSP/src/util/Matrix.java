package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Matrix<T> {
    private List<List<T>> matrix;
    // we only have square matrices
    private final int n;

    public Matrix(int n, T fillValue) {
        this.n = n;
        // fill the matrix with n x n grid of specified fillValue
        this.matrix  = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ArrayList<T> row = new ArrayList<>(Collections.nCopies(n, fillValue));
            this.matrix.add(row);
        }
    }

    public T get(int i, int j) {
        return this.matrix.get(i).get(j);
    }

    public void set(int i, int j, T v) {
        this.matrix.get(i).set(j, v);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.n; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    sb.append(" ");
                }
                String s = this.matrix.get(i).get(j).toString();
                sb.append(s).append(" ".repeat(Math.max(0, 8 - s.length())));
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Matrix<Double> m = new Matrix<>(5, 3.14);
        m.set(2, 1, 42.0);
        System.out.println(m);
    }
}
