package graphReader;

import javafx.util.Pair;
import util.LoggerFormatter;
import util.Matrix;
import util.Triple;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphCompressor {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final GraphReader graphReader;

    // TODO: when extracting path that has been compressed, make many sets so we can map node ID to what ID it's in the
    //       graph or what ID of the 2-edge streams it's in

    /**
     * The passed graphReader must have read the graph as an undirected graph.
     *
     * @param graphReader
     */
    public GraphCompressor(GraphReader graphReader) {
        this.graphReader = removeTwoDegreeNodes(graphReader);
    }

    /**
     * Algorithm:
     * Make a set of all nodes with degree 2
     * Label all vertices as unvisited
     * Do flood-fill on all such nodes, marking nodes as visited
     * When each flood-fill ends, contract the edges and add a new big edge, and remove all the edges found
     *
     * @return a graph reader of the compressed graph
     */
    private GraphReader removeTwoDegreeNodes(GraphReader graphReader) {
        // keep track of new set of edges, starting from the original set
        Set<Triple<Integer, Integer, Double>> edgeSet = new HashSet<>(graphReader.getEdges());

        // used for flood-fill
        List<List<Pair<Integer, Double>>> adjList = graphReader.getAdjacencyList();
        List<Boolean> visited = new ArrayList<>(adjList.size());
        for (int i = 0; i < adjList.size(); i++) {
            visited.add(false);
        }

        // now find all the nodes with just 2-degree
        Set<Integer> twoDegreeNodes = new HashSet<>();
        for (int i = 0; i < graphReader.getNumberOfNodes(); i++) {
            if (adjList.get(i).size() == 2) {
                twoDegreeNodes.add(i);
            }
        }
        LOGGER.info("GraphCompressor starting to compress graph of size " + graphReader.getNumberOfNodes()
                + " where there are " + twoDegreeNodes.size() + " nodes with degree 2");

        // do flood-fill on all of these nodes
        for (int n : twoDegreeNodes) {
            double totalWeight = 0.0;
            // there should only be two nodes with non-2-degree at each end of the sequence
            List<Integer> edgeNodes = new ArrayList<>(2);

            if (visited.get(n)) continue;

            LOGGER.fine("GraphCompressor: Starting flood-fill from node " + n);

            // set up flood-fill
            Queue<Integer> bfsQueue = new LinkedList<>();
            bfsQueue.add(n);
            visited.set(n, true);

            while (!bfsQueue.isEmpty()) {
                int cur = bfsQueue.poll();
                LOGGER.fine("GraphCompressor: Currently looking at neighbours of node " + cur);
                // go through all 2 neighbours
                for (Pair<Integer, Double> next : adjList.get(cur)) {
                    // found edge node
                    if (!twoDegreeNodes.contains(next.getKey())) {
                        // we have a circle with outlier ("Q" pattern)
                        if (edgeNodes.size() == 1 && next.getKey().equals(edgeNodes.get(0))) {
                            // so add this node, even if it's degree is 2
                            edgeNodes.add(cur);
                        } else {
                            edgeNodes.add(next.getKey());
                            // remove the edge we traversed
                            totalWeight += next.getValue();
                            edgeSet.remove(new Triple<>(cur, next.getKey(), next.getValue()));
                            edgeSet.remove(new Triple<>(next.getKey(), cur, next.getValue()));
                        }
                    }
                    // we found another two-degree node
                    else if (!visited.get(next.getKey())){
                        // add to queue
                        visited.set(next.getKey(), true);
                        bfsQueue.add(next.getKey());
                        // delete the edge we traversed
                        totalWeight += next.getValue();
                        edgeSet.remove(new Triple<>(cur, next.getKey(), next.getValue()));
                        edgeSet.remove(new Triple<>(next.getKey(), cur, next.getValue()));
                    }
                }
            }

            // add back a longer edge to compensate for all the edges removed
            if (edgeNodes.size() == 2) {
                edgeSet.add(new Triple<>(edgeNodes.get(0), edgeNodes.get(1), totalWeight));
            } else {
                throw new IllegalStateException("The edgeNodes list should always have 2 elements");
            }
        }
        LOGGER.fine("GraphCompressor compressed edge set to: " + edgeSet.toString());

        GraphReader newGraphReader = new GraphReader(new ArrayList<>(edgeSet), false);
        LOGGER.info("GraphCompressor has completed compression and the resulting graph has size " + newGraphReader.getNumberOfNodes());
        return newGraphReader;
    }

    Matrix<Number> getAdjacencyMatrix() {
        return this.graphReader.getAdjacencyMatrix();
    }

    public GraphReader getGraphReader() {
        return graphReader;
    }

    public static void main(String[] args) {
        LoggerFormatter.setupLogger(LOGGER, Level.INFO);

        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../datasets/OL-but-smaller.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        GraphCompressor graphCompressor = new GraphCompressor(graphReader);
        graphCompressor.getGraphReader().printSummary();
    }
}
