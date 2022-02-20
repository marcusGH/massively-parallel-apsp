package memoryModel.topology;

import java.lang.Math;

public class SquareGridTopology implements Topology {
    private int n;

    public SquareGridTopology(int n) {
        this.n = n;
    }

    @Override
    synchronized public int distance(int i1, int j1, int i2, int j2) {
        int horizontalDistance = Math.min(Math.abs(i2 - i1), n - Math.abs(i2 - i1));
        int verticalDistance   = Math.min(Math.abs(j2 - j1), n - Math.abs(j2 - j1));
        return horizontalDistance + verticalDistance;
    }
}
