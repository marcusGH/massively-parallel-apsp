package graphReader;

import javafx.util.Pair;
import util.Triple;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import util.Matrix;

public class GraphReader {
    private final List<Triple<Integer, Integer, Double>> edges;
    private final Set<Pair<Integer, Integer>> edgeSet;
    private final Set<Integer> nodeIDs;
    private final int n;
    final boolean graphIsDirected;

    private Map<Integer, Integer> nodeIDRemapping;
    private Map<Integer, Integer> nodeIDRemappingInverse;

    public GraphReader(String filename, boolean isDirected) throws ParseException {
        this.graphIsDirected = isDirected;

        // try to open the file
        File file = new File(filename);
        FileReader fr;
        BufferedReader br;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            throw new ParseException(e.getMessage(), 0);
        }

        String line;
        List<Triple<Integer, Integer, Double>> edges = new ArrayList<>();
        try {
            // read the file line  by line
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split("\\s+");
                edges.add(new Triple<>(
                        // The 2nd and 3rd column is the node ID
                        Integer.parseInt(splitted[1]),
                        Integer.parseInt(splitted[2]),
                        // 4th column is weight of edge
                        Double.parseDouble(splitted[3])
                ));
            }
            fr.close();
        } catch (IOException | NumberFormatException e) {
            throw new ParseException("Unable to read and parse line " + (edges.size() + 1) + ": " + e.getMessage(), edges.size());
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ParseException("The line did not have 4 columns: " + e.getMessage(), edges.size());
        }

        // keep track of what node IDs found
        this.nodeIDs = getNodeIDSet(edges);
        this.n = this.nodeIDs.size();
        // save the read data
        this.edges = reIndexEdges(edges);
        // create set for quick edge queries
        this.edgeSet = findEdgeSet(this.edges);
    }

    public GraphReader(List<Triple<Integer, Integer, Double>> edges, boolean isDirected) {
        this.graphIsDirected = isDirected;

        // keep track of what node IDs found
        this.nodeIDs = getNodeIDSet(edges);
        this.n = this.nodeIDs.size();
        // save the read data
        this.edges = reIndexEdges(edges);
        // create set for quick edge queries
        this.edgeSet = findEdgeSet(this.edges);
    }


    /**
     * Does a flood fill from the source and returns all the edges found
     * @param adjacencyList
     * @param visited
     * @param source
     * @return the largest connected component
     */
    private List<Triple<Integer, Integer, Double>> findConnectedComponent(List<List<Pair<Integer, Double>>> adjacencyList,
                                                                          Set<Integer> visited, int source) {
        List<Triple<Integer, Integer, Double>> newEdges = new ArrayList<>();

        // already flood-filled in this component
        if (visited.contains(source)) {
            return newEdges;
        }

        // set up BFS
        Queue<Integer> q = new LinkedList<>();
        q.add(source);
        visited.add(source);

        while (!q.isEmpty()) {
            int cur = q.poll();

            List<Pair<Integer, Double>> neighbours = adjacencyList.get(cur);
            for (Pair<Integer, Double> next : neighbours) {
                // next node not visited
                if (!visited.contains(next.getKey())) {
                    q.add(next.getKey());
                    visited.add(next.getKey());
                }
                // keep track of the edge
                newEdges.add(new Triple<>(cur, next.getKey(), next.getValue()));
            }
        }

        return newEdges;
    }

    private Set<Pair<Integer, Integer>> findEdgeSet(List<Triple<Integer, Integer, Double>> edges) {
        return edges.stream().map(t -> new Pair<>(t.getFirst(), t.getSecond())).collect(Collectors.toSet());
    }

    /**
     * Makes all the edges be indexed from 0 to n-1 where n is the number of edges in the graph
     * @param edges
     * @return
     */
    private List<Triple<Integer, Integer, Double>> reIndexEdges(List<Triple<Integer, Integer, Double>> edges) {
        // maps from old IDs to proposed new reindexed IDs, starting at 0
        Map<Integer, Integer> idMap = new HashMap<>();
        for (Integer i : this.nodeIDs) {
            idMap.put(i, idMap.size());
        }
        // save in case want to recover the mappings
        this.nodeIDRemapping = idMap;
        this.nodeIDRemappingInverse = idMap.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));

        // perform remapping
        return edges.stream().map(triple ->
                new Triple<>(idMap.get(triple.x()), idMap.get(triple.y()), triple.z()))
                .collect(Collectors.toList());
    }

    /**
     * When the graph reader is constructed, all nodes are reindexed to start from 0 and increase by 1. This method
     * can be used to get the mapping from the node id in the graph passed in the constructor to the id in the reindexed
     * graph.
     * @param originalID integer id in original graph
     * @return integer id in internally used graph
     */
    public int getNodeIDAfterReindex(int originalID) {
        if (!this.nodeIDRemapping.containsKey(originalID)) {
            throw new NullPointerException(String.format("The node ID map does not have key %d, only keys: %s", originalID, this.nodeIDRemapping.keySet()));
        }
        return this.nodeIDRemapping.get(originalID);
    }

    public int getNodeIdBeforeReIndex(int newID) {
        return this.nodeIDRemappingInverse.get(newID);
    }

    private Set<Integer> getNodeIDSet(List<Triple<Integer, Integer, Double>> edges) {
        // Create a set of all the distinct node IDs found in the list of edges
        Set<Integer> nodeIds = edges.stream()
                .mapToInt(Triple::x)
                .collect(HashSet::new, HashSet::add, AbstractCollection::addAll);
        nodeIds.addAll(edges.stream()
                .mapToInt(Triple::y)
                .collect(HashSet::new, HashSet::add, AbstractCollection::addAll));
        return nodeIds;
    }

    public int getNumberOfNodes() {
        return this.nodeIDs.size();
    }

    public boolean hasEdge(int i, int j) {
        boolean result = this.edgeSet.contains(new Pair<>(i, j));
        if (this.graphIsDirected) {
            return result;
        } else {
            // check the other direction as well
            return result || this.edgeSet.contains(new Pair<>(j, i));
        }
    }

    public List<List<Pair<Integer, Double>>> getAdjacencyList() {
        // initialize to have a list for each node
        List<List<Pair<Integer, Double>>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        // add entries for each edge
        for (Triple<Integer, Integer, Double> triple : edges) {
            adjList.get(triple.x()).add(new Pair<>(triple.y(), triple.z()));
            if (!this.graphIsDirected) {
                adjList.get(triple.y()).add(new Pair<>(triple.x(), triple.z()));
            }
        }
        return adjList;
    }

    /**
     * If the graph contains multiple edges between the same nodes, the shortest edge is used.
     * @return
     */
    public Matrix<Number> getAdjacencyMatrix() {
        Matrix<Number> mat = new Matrix<>(n, () -> Double.POSITIVE_INFINITY);
        for (Triple<Integer, Integer, Double> e : edges) {
            // in case of multiple edges between same pair of nodes, use minimum weight
            double weight = Math.min(e.z(), mat.get(e.x(), e.y()).doubleValue());
            if (!this.graphIsDirected) {
                weight = Math.min(weight, mat.get(e.y(), e.x()).doubleValue());
                mat.set(e.y(), e.x(), weight);
            }
            mat.set(e.x(), e.y(), weight);
        }
        return mat;
    }

    public List<Triple<Integer, Integer, Double>> getEdges() {
        return edges;
    }

    public void printSummary() {
        System.out.println("Number of edges: " + this.edges.size());
        System.out.println("Number of nodes: " + this.nodeIDs.size());
        System.out.println("Max ID: " +  this.nodeIDs.stream().mapToInt(v -> v).max().orElse(-1));
        System.out.println("Min ID: " +  this.nodeIDs.stream().mapToInt(v -> v).min().orElse(-1));
        System.out.println("Some five edges:");
        for (int i = 0; i < Math.min(5, this.edges.size()); i++) {
            System.out.println("    " + this.edges.get(i));
        }

        // compute statistics from the adjacency list
        List<List<Pair<Integer, Double>>> neighbours = getAdjacencyList();
        System.out.println("Max degree: " + neighbours.stream().mapToInt(List::size).max().orElse(-1));
        System.out.println("Min degree: " + neighbours.stream().mapToInt(List::size).min().orElse(-1));
        System.out.println("Average degree: " + neighbours.stream().mapToInt(List::size).average().orElse(-1));
        IntStream.range(0, 15).forEach(
            i -> System.out.println("Number of nodes with degree " + i + ": " + neighbours.stream().mapToInt(List::size).filter(j -> j == i).count())
        );
    }

    public static void main(String[] args) {
        try {
//            GraphReader gr = new GraphReader("../datasets/small-example.cedge");
            GraphReader gr = new GraphReader("../datasets/OL-but-smaller.cedge", false);
//            GraphCompressor gc = new GraphCompressor(gr);
//            gr = gc.getCompressedGraph();
            gr.printSummary();
//            gr.onlyUseLargestConnectedComponent();
//            System.out.println("The matrix:");
//            System.out.println(gr.getAdjacencyMatrix(false));
        } catch (ParseException e) {
            System.out.println("Encountered parsing error at offset " + e.getErrorOffset());
            e.printStackTrace();
            return;
        }
    }
}
