package APSPSolver;

import graphReader.GraphReader;
import util.Matrix;

import java.util.List;
import java.util.Optional;

public abstract class APSPSolver {

    protected final GraphReader graph;
    protected final boolean graphIsDirected;
    protected final int n;

    private APSPSolver(GraphReader graphReader, int n, boolean graphIsDirected) {
        this.graph = graphReader;
        this.n = n;
        this.graphIsDirected = graphIsDirected;
    }

    public APSPSolver(GraphReader graphReader) {
        this(graphReader, true);
    }

    public APSPSolver(GraphReader graphReader, boolean graphIsDirected) {
        this(graphReader, graphReader.getNumberOfNodes(), graphIsDirected);
    }

    public abstract Optional<List<Integer>> getShortestPath(int i, int j);

    public abstract Number getDistanceFrom(int i, int j);

    public abstract void solve();
}
