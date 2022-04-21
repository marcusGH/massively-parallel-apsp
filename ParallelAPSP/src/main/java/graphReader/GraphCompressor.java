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

    /**
     * Maps from ID in original graph to list of nodes IDs both in the original and compressed graph. The codomain
     * of this mapping uses the original IDs, so they are not reindexed.
     */
    private Map<Integer, List<Integer>> closestNodesInCompressedGraph;
    /**
     * Entry (u, v) maps to a list of nodes L. The edge (u, v) should be present in the compressed graph,
     * and list L contains all the 2-degree nodes that make a path from u to v in the original graph,
     * excluding both nodes u and v.
     * If (u, v) was an edge in the original graph, the list L is simply {}. (to be consistent with the start/end exclusion above)
     */
    private Map<Integer, Map<Integer, List<Integer>>> compressedTwoDegreePaths;
    /**
     * Entry (u, v) maps to the total path length of the list of nodes L, as described in {@link #compressedTwoDegreePaths}
     */
    private Map<Integer, Map<Integer, Double>> compressedTwoDegreePathLengths;

    private Set<Integer> twoDegreeNodes;

    /**
     * Whenever we compress a path U --- a --- b -- ... -- z --- V into an edge (U, V), the nodes {U, V} are added
     * to this set. We need to keep track of these nodes when reconstructing two degree paths ({@link #findTwoDegreePath(int, int)}
     * because we should not search nodes that are also in the compressed graph, and sometimes these include two degree nodes
     * if we have a "Q"-pattern (as described in {@link #removeTwoDegreeNodes(GraphReader)}.
     */
    private Set<Integer> originalNodesUsedAsEdgesInCompression;

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
        this.compressedTwoDegreePathLengths = new HashMap<>();

        // we add all the edges in the original graph
        List<List<Pair<Integer, Double>>> adjList = graphReader.getAdjacencyList();
        for (int u = 0; u < adjList.size(); u++) {
            // consider edge (u, v)
            for (Pair<Integer, Double> edge : adjList.get(u)) {
                // first time finding an edge going out of u
                if (!this.compressedTwoDegreePaths.containsKey(u)) {
                    this.compressedTwoDegreePaths.put(u, new HashMap<>());
                    this.compressedTwoDegreePathLengths.put(u, new HashMap<>());
                }
                // by default, map each edge (u, v) back to itself, but we change this mapping when compressing
//                this.compressedTwoDegreePaths.get(u).put(edge.getKey(), Arrays.asList(u, edge.getKey()));
                this.compressedTwoDegreePaths.get(u).put(edge.getKey(), Collections.emptyList()); // TODO: does this work?
                this.compressedTwoDegreePathLengths.get(u).put(edge.getKey(), edge.getValue());
            }
        }

        // compress the graph, and then create the solver for it
        this.compressedGraph = removeTwoDegreeNodes(graphReader);
        this.solver = solverConstructor.apply(this.compressedGraph);

        if (this.graph.getNumberOfNodes() != (this.compressedGraph.getNumberOfNodes() +
                this.closestNodesInCompressedGraph.keySet().size())) {
            throw new IllegalStateException("The difference in the graph sizes should be equal the the number of removed nodes: "
                + this.graph.getNumberOfNodes() + " != " + this.compressedGraph.getNumberOfNodes() + " + " + this.closestNodesInCompressedGraph.keySet().size());
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
     * Returns a new graph reader that holds the compressed graphs, but with all of the nodes with degree 2
     * removed (with some exceptions). There are also the following side effects: All of the following variables
     * are populated such that shortest paths in the original graph can be reconstructed when the GraphCompressor
     * is used as an APSPSolver:
     * <ul>
     *     <li> {@link #closestNodesInCompressedGraph} </li>
     *     <li> {@link #compressedTwoDegreePathLengths}</li>
     *     <li> {@link #compressedTwoDegreePaths} </li>
     * </ul>
     *
     * <p>
     *     <h2>The algorithm</h2>
     *     We do a flood-fill on the set of nodes with degree 2, keeping track of which vertices that have been
     *     visited, such that we only search through each contagious set of 2-degree nodes once. For each connected set
     *     of 2-degree nodes, we do a DFS with additional functionality:
     *     <ul>
     *         <li>Every time we traverse an edge, we add its weight to an accumulator. Additionally, the edge is
     *         marked for removal, causing it to be removed from the edgeset of the original graph. There's an
     *         exception for Q-patterns, with more details in the code.</li>
     *         <li>We keep track of the removed nodes in a list, such that each edge in the compressed graph map back
     *         to a list of edges in the original graph. The {@link #compressedTwoDegreePaths} keeps track of these
     *         mappings and {@link #compressedTwoDegreePathLengths} keeps track of the associated path length, and is
     *         used to determine which of two possible list of edges should be used when the compressed graph has multiple
     *         edges between one pair of nodes. Additionally, we populate the list {@code removedNodes} in such a way
     *         that the order of nodes is the same as in the original graph, which is made feasible by using DFS instead
     *         of BFS</li>
     *         <li>After the DFS, we create a mapping from each of the removed vertices to their edge nodes, which are
     *         the 3-degree nodes present in both the original- and compressed graph. We also keep a mapping from the
     *         edge nodes to the list of {@code removedNodes} such that we can reconstruct paths later.</li>
     *     </ul>
     * </p>
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

        this.originalNodesUsedAsEdgesInCompression = new HashSet<>();

        // now find all the nodes with just 2-degree
        this.twoDegreeNodes = new HashSet<>();
        for (int i = 0; i < graphReader.getNumberOfNodes(); i++) {
            if (adjList.get(i).size() == 2) {
                this.twoDegreeNodes.add(i);
            }
        }
        LOGGER.info("GraphCompressor starting to compress graph of size " + graphReader.getNumberOfNodes()
                + " where there are " + this.twoDegreeNodes.size() + " nodes with degree 2");

        // maps from removed nodes to list of nodes in both original and compressed graph
        this.closestNodesInCompressedGraph = new HashMap<>();

        // do flood-fill on all of the nodes with degree 2
        for (int n : this.twoDegreeNodes) {
            // accumulate the total weight of the all the edges removed
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
                    // found edge node (not used before)
                    if (!this.twoDegreeNodes.contains(next.getKey())) {
                        // we have a circle with outlier ("Q" pattern):
                        //        A ----- D ----- E . . .
                        //        |       |
                        //        B-------C
                        // in that case, we want to mark both D and either A or C as edge nodes,
                        //   and D is already an edge node so we mark A or C (whatever is cur)
                        if (edgeNodes.size() == 1 && next.getKey().equals(edgeNodes.get(0))) {
                            // add current node, even if it's degree is 2
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
                        // when doing DFS on a set of nodes each with at most degree 2, we will have the following:
                        //   o --- o --- o --- o --- o --- R ---- ....
                        //   \_____/     ^           ^
                        //      A       source       cur
                        //
                        //  The size of dfsQueue will be 2 if we have not yet explored A, and size 1 if we have.
                        //  By appending to the start or end of the list based on whether we have explored nodes A or
                        //    not, we can reconstruct the list of removed nodes in correct order.
                        //  There is also an edge case where R is an edge node, but we have not explored A yet, in which
                        //    case the edgeNodes size is 1, but we should still append at the end
                        // We have not explored the other branch of the source node (A)
                        if (dfsQueue.size() == 2 || (edgeNodes.size() == 1 && edgeNodes.get(0).equals(next.getKey()))) {
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
                this.originalNodesUsedAsEdgesInCompression.addAll(edgeNodes);
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
                    System.out.println(removedNodes);
                    for (int k : removedNodes) {
                        System.out.println(String.format("Neighbours of node %d: %s", k, adjList.get(k)));
                    }
                    throw new IllegalStateException("The list of removed nodes should constitute a path");
                }
            }
            // don't add the compressed path back if there is a shorter edge in the multigraph, and add one of course
            //   we don't have this edge. We add the -\infty default so that we enter the branch of the second key does
            //   not exist i.e. we don'thave the edge
            if (null != this.compressedTwoDegreePathLengths && (!this.compressedTwoDegreePathLengths.containsKey(edgeNodes.get(0)) ||
                    totalWeight < this.compressedTwoDegreePathLengths.get(edgeNodes.get(0)).getOrDefault(edgeNodes.get(1), Double.POSITIVE_INFINITY))) {
                // add the list in the appropriate order
                if (!removedNodes.getFirst().equals(edgeNodes.get(0))) {
                    Collections.reverse(removedNodes);
                }
                this.compressedTwoDegreePaths.get(edgeNodes.get(0)).put(edgeNodes.get(1), removedNodes);
                List<Integer> removedNodesRev = new LinkedList<>(removedNodes);
                Collections.reverse(removedNodesRev);
                this.compressedTwoDegreePaths.get(edgeNodes.get(1)).put(edgeNodes.get(0), removedNodesRev);
                // keep track of the path length to avoid multi-edge graphs and make sure we only keep shortest edge
                this.compressedTwoDegreePathLengths.get(edgeNodes.get(0)).put(edgeNodes.get(1), totalWeight);
                this.compressedTwoDegreePathLengths.get(edgeNodes.get(1)).put(edgeNodes.get(0), totalWeight);
            }
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
     * as intermediate nodes. This search is done on the original graph, so original node IDs
     * are used, not the reindexed node IDs in the compressed graph.
     *
     * @param i start node
     * @param j end node
     * @return a tuple of the length of the path and a list of nodes
     * in the path, <strong>excluding</strong> both the start and end nodes
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
                // We also need to exclude nodes used as edge nodes during compression. This is not disjoint with the
                //   two degree nodes because we may have a Q-pattern edge case
                else if (this.twoDegreeNodes.contains(next.getKey()) && !visited.contains(next.getKey())
                        && !this.originalNodesUsedAsEdgesInCompression.contains(next.getKey())) {
                    queue.add(next.getKey());
                    visited.add(next.getKey());
                    prev.put(next.getKey(), cur);
                    dist.put(next.getKey(), d);
                }
            }
        }

        // there is no path
        if (!prev.containsKey(j)) {
            return new Pair<>(Integer.MAX_VALUE, Collections.emptyList());
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

    /**
     * Returns the shortest path and its length from i to j in the original graph.
     *
     * <p>
     *     <h3>The algorithm</h3>
     *     For both i and j, we have one of two cases: The node is not in the original graph, in which case we consider
     *     the two closest nodes to it that are both in the compressed and original graph. The other case is that it's
     *     in the original graph, in which case we only consider the node itself. These cases are implemented in the
     *     construction of {@link #closestNodesInCompressedGraph}.
     * <br>
     *     We then iterate all the cases and consider all paths on the form i -> start -> end -> j, where start and end
     *     are the considered nodes described above such that they are in the compressed graph. We find the length of
     *     the path and reconstruct it by using the auxiliary method {@link #findTwoDegreePath(int, int)} for paths
     *     i -> start and end -> j, and use the state stored in {@link #compressedTwoDegreePaths} together with the
     *     APSPSolver {@link #solver} to extrapolate the paths solved for the compressed graph onto paths in the original
     *     graph. This is done by using all the same edges in the solver's solution, but changing the indices to those
     *     before compression as well as expanding compressed edges to the list of nodes that were in the original graph.
     * </p>
     *
     * @param i start node ID (in original graph)
     * @param j end node ID (in original graph)
     * @return a tuple (n, L), where n is the length of the path between i and j in the original graph, and
     * L is the list of nodes constituting the path, on the form [i, ..., j]. We return L = Optional.empty()
     * if no such path exists.
     */
    private Pair<Number, Optional<List<Integer>>> getShortestDistanceAndPathAux(int i, int j) {
        if (null == this.solver) {
            throw new IllegalStateException("This method is not available if a APSPSolver has not been provided upon construction");
        }

        // no paths should be longer than this
        double shortestDist = Integer.MAX_VALUE;
        List<Integer> shortestPath = null;

        // we have an edge case where the i and j are connected through only two degree nodes that have been removed
        Pair<Number, List<Integer>> intermediatePath = findTwoDegreePath(i, j);
        LOGGER.fine("PathReconstruction: Found intermediate path (" + i + ", " + j + "): " + intermediatePath);
        if (intermediatePath.getKey().doubleValue() < shortestDist) {
            shortestPath = new ArrayList<>();
            shortestPath.add(i);
            shortestPath.addAll(intermediatePath.getValue());
            shortestPath.add(j);
            shortestDist = intermediatePath.getKey().doubleValue();
            LOGGER.fine("PathReconstruction: Using intermediate two-degree paths between " + i + " and " + j);
        }

        // if both nodes and in the compressed graph, this only does a single iteration with start == i and end == j
        for (int start : this.closestNodesInCompressedGraph.get(i)) {
            for (int end : this.closestNodesInCompressedGraph.get(j)) {
                LOGGER.fine("PathReconstruction: using start=" + start + " and end=" + end);
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
        // We count a self-loop as there being no path
        if (i == j || result.getValue().isPresent() && result.getValue().get().size() == 1) {
            return Optional.empty();
        } else {
            return result.getValue();
        }
    }

    @Override
    public Number getDistanceFrom(int i, int j) {
        if (i == j) {
            return 0.0;
        }
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
        System.out.println(graphCompressor.getShortestPath(1, 7));
        System.out.println(graphCompressor.getDistanceFrom(1, 7));

        // Current bugs:
        // * If there are multiples of an edge, we might need to use the edge from the original graph instead of the
        //   compressed on if it is shorter. However, this will be a pain to fix, so just assume the uncompressed edge
        //   is shorter in those cases by creating a check for Graph::hasEdge in the middle path thing
        // * When uncompressing the edges, the order may be reversed. To fix this, check if hasEdge start middle.get(0)
        //   and if not, just reverse the list before adding it
    }
}
