package parallelAPSP;

import graphReader.GraphReader;
import util.Matrix;

import java.text.ParseException;
import java.util.List;

public abstract class ParallelAPSP {
    private final boolean graphIsDirected;

    protected final int n;

    protected final Matrix<Number> adjacencyMatrix;
    protected final Matrix<Integer> predecessorMatrix;
    protected final Matrix<Number> weightMatrix;

    private ParallelAPSP(int n) {
        // TODO: add paramter
        this.n = n;
        this.graphIsDirected = true;
        this.adjacencyMatrix = new Matrix<>(n);
        this.predecessorMatrix = new Matrix<>(n);
        this.weightMatrix = new Matrix<>(n);
    }

    public ParallelAPSP(Matrix<? extends Number> adjacencyMatrix) {
        this(adjacencyMatrix.size());
        // cast everything to Numbers
        for (int i = 0; i < adjacencyMatrix.size(); i++) {
            for (int j = 0; j < adjacencyMatrix.size(); j++) {
                this.adjacencyMatrix.set(i, j, adjacencyMatrix.get(i, j));
            }
        }
        // set 0s in the diagonal
        for (int i = 0; i < adjacencyMatrix.size(); i++) {
            this.adjacencyMatrix.set(i, i, 0);
        }
    }

    public List<Integer> getShortestPath(int i, int j) {
        throw new RuntimeException("Not implemented yet");
    }

    public Number getDistanceFrom(int i, int j) {
        // TODO: check if computation done first
        return this.weightMatrix.get(i, j);
    }

    abstract public void solve();
}
