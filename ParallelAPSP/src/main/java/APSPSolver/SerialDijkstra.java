package APSPSolver;

import graphReader.GraphReader;
import javafx.util.Pair;

import java.util.*;
import java.util.logging.Logger;

public class SerialDijkstra extends APSPSolver {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private List<List<Double>> distances;
    private List<List<Integer>> predecessors;

    public SerialDijkstra(GraphReader graphReader) {
        super(graphReader);
    }

    @Override
    public Optional<List<Integer>> getShortestPath(int i, int j) {
        if (this.predecessors == null) {
            throw new IllegalStateException("The solve method must be called before querying shortest paths");
        }
        List<Integer> preds = this.predecessors.get(i);

        if (preds.get(j) == -1 || i == j) {
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

        List<List<Pair<Integer, Double>>> neighbours = this.graph.getAdjacencyList();
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
            prev.set(source, source);
            distance.set(source, 0.0);

            priorityQueue.add(new Pair<>(source, 0.0));
            while (!priorityQueue.isEmpty()) {
                Pair<Integer, Double> cur = priorityQueue.poll();

                LOGGER.finer("SerialDijkstra: Inspecting node " + cur.getKey() + " and distance " + cur.getValue());
                // we have already looked at this node
                if (cur.getValue() > distance.get(cur.getKey())) {
                    continue;
                }

                // go through all the neighbour edges of node next
                for (Pair<Integer, Double> next : neighbours.get(cur.getKey())) {
                    // do relaxation
                    double newDist = cur.getValue() + next.getValue();
                    if (newDist < distance.get(next.getKey())) {
                        distance.set(next.getKey(), newDist);
                        prev.set(next.getKey(), cur.getKey());
                        priorityQueue.add(new Pair<>(next.getKey(), newDist));
                    }
                }
            }
            LOGGER.finer("SerialDijkstra computed distances for source=" + source + " to be: " + distance);
            LOGGER.finer("SerialDijkstra computed preds for source=" + source + " to be: " + prev);
            // store the results
            this.distances.add(distance);
            this.predecessors.add(prev);
        }
    }
}
