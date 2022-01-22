package APSPSolver;

import graphReader.GraphReader;
import util.Matrix;

import java.util.List;
import java.util.Optional;

public abstract class APSPSolver {

    protected final GraphReader graph;
    protected final int n;

    private APSPSolver(GraphReader graphReader, int n) {
        this.graph = graphReader;
        this.n = n;
    }

    public APSPSolver(GraphReader graphReader) {
        this(graphReader, graphReader.getNumberOfNodes());
    }

    public abstract Optional<List<Integer>> getShortestPath(int i, int j);

    public abstract Number getDistanceFrom(int i, int j);

    public abstract void solve();
}
