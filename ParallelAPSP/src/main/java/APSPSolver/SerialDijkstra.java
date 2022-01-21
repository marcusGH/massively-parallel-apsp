package APSPSolver;

import graphReader.GraphReader;
import javafx.util.Pair;

import java.util.*;

public class SerialDijkstra extends APSPSolver {

    private List<List<Double>> distances;
    private List<List<Integer>> predecessors;

    public SerialDijkstra(GraphReader graphReader, boolean graphIsDirected) {
        super(graphReader, graphIsDirected);
    }

    @Override
    public Optional<List<Integer>> getShortestPath(int i, int j) {
        if (this.predecessors == null) {
            throw new IllegalStateException("The solve method must be called before querying shortest paths");
        }
        List<Integer> preds = this.predecessors.get(i);

        if (preds.get(j) == -1) {
            return Optional.empty();
        }

        Deque<Integer> path = new LinkedList<>();
        do {
            if (preds.get(j) == j) {
                throw new IllegalStateException(String.format("The predecessor list should not have self-references: Pred(%d, %d)=%d", i, j, j));
            }
            path.addFirst(j);
            j = preds.get(j);
        } while (i !=  j);
        path.addFirst(i);
        return Optional.of(new ArrayList<>(path));
    }

    @Override
    public Number getDistanceFrom(int i, int j) {
        if (this.distances == null) {
            throw new IllegalStateException("The solve method must be called before querying shortest paths length");
        }
        return this.distances.get(i).get(j);
    }

    @Override
    public void solve() {
        this.distances = new ArrayList<>();
        this.predecessors = new ArrayList<>();

        List<List<Pair<Integer, Double>>> neighbours = this.graph.getAdjacencyList(this.graphIsDirected);
        // solve Dijkstra once for each origin node
        for (int source = 0; source < this.n; source++) {

            // sort by second element in non-decreasing order
            PriorityQueue<Pair<Integer, Double>> priorityQueue =
                    new PriorityQueue<>(Comparator.comparing(Pair::getValue));

            // initial conditions
            List<Double> distance = new ArrayList<>(this.n);
            List<Integer> prev = new ArrayList<>(this.n);
            for (int i = 0; i < this.n; i++) {
                distance.add(Double.POSITIVE_INFINITY);
                prev.add(-1);
            }
            distance.set(source, 0.0);
            prev.set(source, source);

            priorityQueue.add(new Pair<>(source, 0.0));
            while (!priorityQueue.isEmpty()) {
                Pair<Integer, Double> cur = priorityQueue.poll();

                if (cur.getValue() < distance.get(cur.getKey())) {
                    distance.set(cur.getKey(), cur.getValue());
                }
                // we have already looked at this node
                else {
                    continue;
                }

                // go through all the neighbour edges of node next
                for (Pair<Integer, Double> next : neighbours.get(cur.getKey())) {
                    // do relaxation
                    double newDist = cur.getValue() + next.getValue();
                    if (newDist < cur.getValue()) {
                        prev.set(next.getKey(), cur.getKey());
                        priorityQueue.add(new Pair<>(next.getKey(), newDist));
                    }
                }
            }
            // store the results
            this.distances.add(distance);
            this.predecessors.add(prev);
        }
    }
}
