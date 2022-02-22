package graphReader;

import APSPSolver.APSPSolver;
import APSPSolver.RepeatedMatrixSquaring;
import javafx.util.Pair;
import matrixMultiplication.GeneralisedFoxOtto;
import util.LoggerFormatter;
import util.Triple;

import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphCompressor extends APSPSolver {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final APSPSolver solver;
    private final GraphReader compressedGraph;

    // Maps from ID in original graph to list of IDs in original graph of nodes that are also
    //   present in the compressed graph
    private Map<Integer, List<Integer>> closestNodesInCompressedGraph;
    // Entry (u, v) maps to a list of nodes L. The edge (u, v) should be present in the compressed graph,
    //   and list L contains all the 2-degree nodes that make a path from u to v in the original graph.
    //   If (u, v) was an edge in the original graph, the list L is simply {u, v}.
    private Map<Integer, Map<Integer, List<Integer>>> compressedTwoDegreePaths;
    private Set<Integer> twoDegreeNodes;

    // TODO: when extracting path that has been compressed, make many sets so we can map node ID to what ID it's in the
    //       graph or what ID of the 2-edge streams it's in

    /**
     * The passed graphReader must have read the graph as an <strong>undirected</strong> graph. This constructor
     * is used if we are only interested in compressing a graph and not trying to recover any information from the
     * original graph, such as shortest paths and shortest distances.
     *
     * @param graphReader a graph
     */
    public GraphCompressor(GraphReader graphReader) {
        super(graphReader);
        this.compressedGraph = removeTwoDegreeNodes(graphReader);
        this.solver = null;

        // we do not need to sanity check the closestNodesInCompressedGraph and twoDegreeNodes states, as that will
        //   not be used with this constructor. Here we are not interested in reconstructing paths.
    }

    /**
     * This constructor is used if we want to compress the provided graph, solve APSP on the compressed graph, then
     * recover the paths and shortest distances in the original graph.
     *
     * @param graphReader an <strong>undirected</strong> graoh.
     * @param solverConstructor a APSPSolver constructor
     */
    public GraphCompressor(GraphReader graphReader, Function<GraphReader, ? extends APSPSolver> solverConstructor) {
        super(graphReader);
        this.compressedTwoDegreePaths = new HashMap<>();

        // we add all the edges in the original graph
        List<List<Pair<Integer, Double>>> adjList = graphReader.getAdjacencyList();
        for (int u = 0; u < adjList.size(); u++) {
            // consider edge (u, v)
            for (Pair<Integer, Double> edge : adjList.get(u)) {
                // first time finding an edge going out of u
                if (!this.compressedTwoDegreePaths.containsKey(u)) {
                    this.compressedTwoDegreePaths.put(u, new HashMap<>());
                }
                // by default, map each edge (u, v) back to itself, but we change this mapping when compressing
                this.compressedTwoDegreePaths.get(u).put(edge.getKey(), Arrays.asList(u, edge.getKey()));
            }
        }

        // compress the graph, and then create the solver for it
        this.compressedGraph = removeTwoDegreeNodes(graphReader);
        this.solver = solverConstructor.apply(this.compressedGraph);

        if (this.graph.getNumberOfNodes() != (this.compressedGraph.getNumberOfNodes() +
                this.closestNodesInCompressedGraph.keySet().size())) {
            throw new IllegalStateException("The difference in the graph sizes should be equal the the number of removed nodes");
        }
        // for all the nodes that were not removed, we create a mapping to themselves
        for (int i = 0; i < this.graph.getNumberOfNodes(); i++) {
            if (!this.closestNodesInCompressedGraph.containsKey(i)) {
                this.closestNodesInCompressedGraph.put(i, Collections.singletonList(i));
            }
        }

    }

    /**
     * Utility function for partially applying the generalized fox otto matrix multiplication solver so that it
     * can be passed to the graph compressor constructor.
     *
     * @param p number of rows and columns of processing elements to use in the solver
     * @return partially applied constructor for a {@link RepeatedMatrixSquaring} solver.
     */
    public static Function<GraphReader, ? extends APSPSolver> getCurriedFoxOttoAPSPSolverConstructor(int p) {
        return (graphReader -> new RepeatedMatrixSquaring(graphReader, p, GeneralisedFoxOtto.class));
    }

    /**
     * Algorithm:
     * Make a set of all nodes with degree 2
     * Label all vertices as unvisited
     * Do flood-fill on all such nodes, marking nodes as visited
     * When each flood-fill ends, contract the edges and add a new big edge, and remove all the edges found
     * If we end up with multiples edges between a pair of nodes, all the edges are stored, but all but the shortest
     * edge will be removed later in {@link GraphReader#getAdjacencyMatrix()}.
     *
     * @return a graph reader of the compressed graph
     */
    private GraphReader removeTwoDegreeNodes(GraphReader graphReader) {
        if (graphReader.graphIsDirected) {
            throw new IllegalArgumentException("The provided graph is directed, so it cannot be compressed");
        }

        // keep track of new set of edges, starting from the original set
        Set<Triple<Integer, Integer, Double>> edgeSet = new HashSet<>(graphReader.getEdges());

        // used for flood-fill
        List<List<Pair<Integer, Double>>> adjList = graphReader.getAdjacencyList();
        List<Boolean> visited = new ArrayList<>(adjList.size());
        for (int i = 0; i < adjList.size(); i++) {
            visited.add(false);
        }

        // now find all the nodes with just 2-degree
        this.twoDegreeNodes = new HashSet<>();
        for (int i = 0; i < graphReader.getNumberOfNodes(); i++) {
            if (adjList.get(i).size() == 2) {
                this.twoDegreeNodes.add(i);
            }
        }
        LOGGER.info("GraphCompressor starting to compress graph of size " + graphReader.getNumberOfNodes()
                + " where there are " + this.twoDegreeNodes.size() + " nodes with degree 2");

        this.closestNodesInCompressedGraph = new HashMap<>();

        // do flood-fill on all of these nodes
        for (int n : this.twoDegreeNodes) {
            double totalWeight = 0.0;
            // there should only be two nodes with non-2-degree at each end of the sequence
            List<Integer> edgeNodes = new ArrayList<>(2);

            if (visited.get(n)) continue;

            LOGGER.fine("GraphCompressor: Starting flood-fill from node " + n);

            // set up flood-fill (we do DFS so that there's at most 2 different contiguous paths)
            Stack<Integer> dfsQueue = new Stack<>();
            dfsQueue.add(n);
            visited.set(n, true);

            // so that we can later recover paths
            LinkedList<Integer> removedNodes = new LinkedList<>();

            while (!dfsQueue.isEmpty()) {
                int cur = dfsQueue.pop();
                LOGGER.fine("GraphCompressor: Currently looking at neighbours of node " + cur);
                // go through all 2 neighbours
                for (Pair<Integer, Double> next : adjList.get(cur)) {
                    LOGGER.fine("GraphCompressor:   Looking at neighbour " + next.getKey());
                    boolean removedNode;
                    // found edge node
                    if (!this.twoDegreeNodes.contains(next.getKey())) {
                        // we have a circle with outlier ("Q" pattern)
                        if (edgeNodes.size() == 1 && next.getKey().equals(edgeNodes.get(0))) {
                            // so add this node, even if it's degree is 2
                            edgeNodes.add(cur);
                            removedNode = false;
                        } else {
                            edgeNodes.add(next.getKey());
                            removedNode = true;
                        }
                    }
                    // we found another two-degree node
                    else if (!visited.get(next.getKey())){
                        // add to queue
                        visited.set(next.getKey(), true);
                        dfsQueue.add(next.getKey());
                        removedNode = true;
                    }
                    // we found a node we already visited
                    else {
                        removedNode = false;
                    }

                    // remove the edge we traversed if we deleted a node
                    if (removedNode) {
                        totalWeight += next.getValue();
                        edgeSet.remove(new Triple<>(cur, next.getKey(), next.getValue()));
                        edgeSet.remove(new Triple<>(next.getKey(), cur, next.getValue()));
                        // when starting flood fill on the source s, we will consider both edges (s, u) and (s, v)
                        //   but we only remove node s once, so skip the second add of s
                        if (removedNodes.size() == 1 && removedNodes.getFirst() == cur) {
                            continue;
                        }
                        // we have not explored the other branch of the source node
                        if (dfsQueue.size() == 2) {
                            removedNodes.addLast(cur);
                        } else if (dfsQueue.size() <= 1) {
                            removedNodes.addFirst(cur);
                        } else {
                            throw new IllegalStateException("When DFSing on two-degree nodes, there should only" +
                                    " every be at most two branches to consider.");
                        }
                    }
                }
            }

            // add back a longer edge to compensate for all the edges removed
            if (edgeNodes.size() == 2) {
                edgeSet.add(new Triple<>(edgeNodes.get(0), edgeNodes.get(1), totalWeight));
            } else {
                throw new IllegalStateException("The edgeNodes list should always have 2 elements");
            }
            // also keep a mapping from the removed nodes to the edge nodes so we can reconstruct paths later
            for (int i : removedNodes) {
                this.closestNodesInCompressedGraph.put(i, edgeNodes);
            }
            // for new edges, create a map to a list of all the removed edges, but first verify that the list is a path
            for (int i = 1; i < removedNodes.size(); i++) {
                if (!graphReader.hasEdge(removedNodes.get(i - 1), removedNodes.get(i))) {
                    throw new IllegalStateException("The list of removed nodes should constitute a path");
                }
            }
            this.compressedTwoDegreePaths.get(edgeNodes.get(0)).put(edgeNodes.get(1), removedNodes);
            this.compressedTwoDegreePaths.get(edgeNodes.get(1)).put(edgeNodes.get(0), removedNodes);
        }

        LOGGER.fine("GraphCompressor compressed edge set to: " + edgeSet.toString());

        GraphReader newGraphReader = new GraphReader(new ArrayList<>(edgeSet), false);
        LOGGER.info("GraphCompressor has completed compression and the resulting graph has size " + newGraphReader.getNumberOfNodes());
        return newGraphReader;
    }

    public GraphReader getCompressedGraph() {
        return this.compressedGraph;
    }

    /**
     * Finds the shortest path from i to j under the assumption that a path of
     * length 0 or longer exists from i to j traversing only nodes of degree 2
     * as intermediate nodes.
     *
     * @param i start node
     * @param j end node
     * @return a tuple of the length of the path and a list of nodes
     * in the path, excluding both the start and end nodes
     */
    private Pair<Number, List<Integer>> findTwoDegreePath(int i, int j) {
        if (i == j) {
            return new Pair<>(0.0, new ArrayList<>());
        }

        List<List<Pair<Integer, Double>>> adjList = this.graph.getAdjacencyList();

        // setup BFS data structures
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> prev = new HashMap<>();
        Map<Integer, Double> dist = new HashMap<>();
        // initialise from first node
        visited.add(i);
        dist.put(i, 0.0);
        queue.add(i);

        // do BFS
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            // go through neighbours
            for (Pair<Integer, Double> next : adjList.get(cur)) {
                double d = dist.get(cur) + next.getValue();
                // found destination
                if (next.getKey() == j) {
                    prev.put(j, cur);
                    dist.put(j, d);
                    break;
                }
                // neighbour that is not visited yet (only consider two degree nodes because such a path
                //   should exist)
                else if (this.twoDegreeNodes.contains(next.getKey()) && !visited.contains(next.getKey())) {
                    queue.add(next.getKey());
                    visited.add(next.getKey());
                    prev.put(next.getKey(), cur);
                    dist.put(next.getKey(), d);
                }
            }
        }

        // reconstruct the path
        LinkedList<Integer> path = new LinkedList<>();
        path.addFirst(prev.get(j));
        while (path.getFirst() != i) {
            path.addFirst(prev.get(path.getFirst()));
        }
        // remove first node because exclusive path
        path.removeFirst();

        return new Pair<>(dist.get(j), path);
    }

    private Pair<Number, Optional<List<Integer>>> getShortestDistanceAndPathAux(int i, int j) {
        if (null == this.solver) {
            throw new IllegalStateException("This method is not available if a APSPSolver has not been provided upon construction");
        }

        // no paths should be longer than this
        double shortestDist = Integer.MAX_VALUE;
        List<Integer> shortestPath = null;

        // if both nodes and in the compressed graph, this only does a single iteration with start == i and end == j
        for (int start : this.closestNodesInCompressedGraph.get(i)) {
            for (int end : this.closestNodesInCompressedGraph.get(j)) {
                // both of these paths do not include the start and end node
                Pair<Number, List<Integer>> pathStart = findTwoDegreePath(i, start);
                Pair<Number, List<Integer>> pathEnd = findTwoDegreePath(end, j);
                // the compressed graph is reindexed, so these are the IDs originally used in uncompressed graph
                int startCompressedID = this.compressedGraph.getNodeIDAfterReindex(start);
                int endCompressedID = this.compressedGraph.getNodeIDAfterReindex(end);

                // pathLength = (distance from start to end in the compressed graph)
                //              + (distance from i to start) + (distance from end to j)
                double pathLength = pathStart.getKey().doubleValue() + pathEnd.getKey().doubleValue() +
                        this.solver.getDistanceFrom(startCompressedID, endCompressedID).doubleValue();
                if (pathLength < shortestDist) {
                    shortestDist = pathLength;
                    shortestPath = new ArrayList<>();
                    // start path
                    shortestPath.add(i);
                    shortestPath.addAll(pathStart.getValue());
                    LOGGER.fine("PathReconstruction: Path after start: " + shortestPath);
                    // middle path
                    Optional<List<Integer>> middlePath = this.solver.getShortestPath(startCompressedID, endCompressedID);
                    // if the edge nodes are equal, we have a middle path of length 0 that creates a full valid path
                    if (middlePath.isEmpty() && start == end) {
                        shortestPath.add(start);
                    }
                    else if (middlePath.isEmpty()) {
                        throw new IllegalStateException("There should be a path from start to end when a better distance was found");
                    } else {
                        // we might have an edge that was not present in the original graph, so we translate all the edges
                        //   to either its original index edge or a list of edges in the original graph if it was compressed
                        for (int node = 1; node < middlePath.get().size(); node++) {
                            List<Integer> middlePathList = new ArrayList<>(middlePath.get());
                            LOGGER.fine("PathReconstruction:   Middle path list: " + middlePathList);
                            int beforeNode = this.compressedGraph.getNodeIdBeforeReIndex(middlePathList.get(node - 1));
                            int afterNode = this.compressedGraph.getNodeIdBeforeReIndex(middlePathList.get(node));
                            shortestPath.add(beforeNode);
                            shortestPath.addAll(this.compressedTwoDegreePaths.get(beforeNode).get(afterNode));
                            shortestPath.add(afterNode);
                        }
                    }
                    LOGGER.fine("PathReconstruction: Path after middle: " + shortestPath);
                    // end path
                    shortestPath.addAll(pathEnd.getValue());
                    shortestPath.add(j);

                    // remove repeats
                    Set<Integer> set = new LinkedHashSet<>(shortestPath);
                    shortestPath.clear();
                    shortestPath.addAll(set);

                    LOGGER.fine("PathReconstruction: Complete path: " + shortestPath);
//                    System.out.println(pathStart);
//                    System.out.println(middlePath);
//                    System.out.println(pathEnd);
//                    System.out.println("-");
                }
            }
        }
        // no path found
        if (null == shortestPath) {
            return new Pair<>(Double.POSITIVE_INFINITY, Optional.empty());
        } else {
            return new Pair<>(shortestDist, Optional.of(shortestPath));
        }
    }

    @Override
    public Optional<List<Integer>> getShortestPath(int i, int j) {
        Pair<Number, Optional<List<Integer>>> result = getShortestDistanceAndPathAux(i, j);
        return result.getValue();
    }

    @Override
    public Number getDistanceFrom(int i, int j) {
        Pair<Number, Optional<List<Integer>>> result = getShortestDistanceAndPathAux(i, j);
        return result.getKey();
    }

    @Override
    public void solve() {
        if (null == this.solver) {
            throw new IllegalStateException("This method is not available if a APSPSolver has not been provided upon construction");
        }
        // solve the problem on the compressed graph
        this.solver.solve();
    }

    public static void main(String[] args) {
        LoggerFormatter.setupLogger(LOGGER, Level.FINE);

        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../datasets/compressor-example.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        GraphCompressor graphCompressor = new GraphCompressor(graphReader, getCurriedFoxOttoAPSPSolverConstructor(4));
        graphCompressor.solve();
        System.out.println(graphCompressor.getCompressedGraph().getEdges());
        System.out.println(graphCompressor.getShortestPath(3, 7));
        System.out.println(graphCompressor.getDistanceFrom(3, 7));

        // Current bugs:
        // * If there are multiples of an edge, we might need to use the edge from the original graph instead of the
        //   compressed on if it is shorter. However, this will be a pain to fix, so just assume the uncompressed edge
        //   is shorter in those cases by creating a check for Graph::hasEdge in the middle path thing
        // * When uncompressing the edges, the order may be reversed. To fix this, check if hasEdge start middle.get(0)
        //   and if not, just reverse the list before adding it
    }
}
