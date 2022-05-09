package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Matrix<T> {
    private List<List<T>> matrix;
    // we only have square matrices
    private final int n;

    /**
     * Creates an empty matrix of size n x n. Any call to getters on an empty matrix will
     * produce a NullPointerException because all entries are set to {@code null}.
     *
     * @param n size of the square matrix
     */
    public Matrix(int n) {
        this.n = n;
        this.matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<T> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                row.add(null);
            }
            this.matrix.add(row);
        }
    }

    public Matrix(int n, Supplier<T> defaultValueSupplier) {
        this.n = n;
        // fill the matrix with n x n grid of specified fillValue
        this.matrix  = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            // invoke supplier n times
            List<T> row = Stream.generate(defaultValueSupplier).limit(n).collect(Collectors.toList());
            this.matrix.add(row);
        }
    }

    public Matrix(int n, T[][] startValues) {
        assert startValues.length == n;
        assert startValues[0].length == n;

        this.n = n;
        this.matrix = new ArrayList<>();
        for (int i = 0; i < this.n; i++) {
            this.matrix.add(Arrays.asList(startValues[i]));
        }
    }

    /**
     * Make a deep copy
     * @param matrix a matrix to be copied
     */
    public Matrix(Matrix<T> matrix) {
        this.n = matrix.n;

        this.matrix = new ArrayList<>(this.n);
        for (int i = 0; i < this.n; i++) {
            this.matrix.add(new ArrayList<>(this.n));
            for (int j = 0; j < this.n; j++) {
                this.matrix.get(i).add(matrix.get(i, j));
            }
        }
    }

    public T get(int i, int j) {
        return this.matrix.get(i).get(j);
    }

    public void set(int i, int j, T v) {
        this.matrix.get(i).set(j, v);
    }

    public void setRow(int i, List<T> values) {
        assert values.size() == this.n;
        for (int j = 0; j < this.n; j++) {
            this.set(i, j, values.get(j));
        }
    }

    public void setCol(int j, List<T> values) {
        assert values.size() == this.n;
        for (int i = 0; i < this.n; i++) {
            this.set(i, j, values.get(i));
        }
    }

    public void setAll(Supplier<T> valueSupplier) {
        // fill the matrix with n x n grid of specified fillValue
        for (int i = 0; i < n; i++) {
            // invoke supplier n times
            List<T> row = Stream.generate(valueSupplier).limit(n).collect(Collectors.toList());
            this.matrix.set(i, row);
        }
    }

    public List<T> toList() {
        // flatten the list by merging streams
        return this.matrix.stream().flatMap(List::stream).collect(Collectors.toList());
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
                String s;
                T element = this.matrix.get(i).get(j);
                // special case for numbers because might be infinite
                if (element instanceof Number && (
                        (element instanceof Double && ((Double) element).isInfinite()) ||
                         element instanceof Integer && (Integer) element == Integer.MAX_VALUE ||
                         element instanceof Double && (Math.abs((Double) element - Integer.MAX_VALUE) < 1E-5))) {
                    // abbreviate if it  looks like it's infinite
                    s = "INF";
                } else {
                    s = element.toString();
                }
                sb.append(s).append(" ".repeat(Math.max(0, 8 - s.length())));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Matrix<?>)) {
            return false;
        }
        Matrix<?> other = (Matrix<?>) obj;
        if (other.n != this.n) {
            return false;
        }
        for (int i = 0; i < this.n; i++) {
            for (int j = 0; j < this.n; j++) {
                if (!other.get(i, j).equals(this.get(i, j))) {
                    return false;
                }
            }
        }
        return true;
    }

    public int size() {
        return this.n;
    }

    public static void main(String[] args) {
        Matrix<Double> m = new Matrix<>(5, () -> 3.14);
        m.set(2, 1, 42.0);
        System.out.println(m);
    }
}
