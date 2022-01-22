package graphReader;

import javafx.util.Pair;
import util.Triple;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import util.Matrix;

/**
 * TODO: write a bit more documentation on the methods here
 */
public class GraphReader {
    private final List<Triple<Integer, Integer, Double>> edges;
    private final Set<Pair<Integer, Integer>> edgeSet;
    private final Set<Integer> nodeIDs;
    private final int n;
    final boolean graphIsDirected;

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
        // perform remapping
        return edges.stream().map(triple ->
                new Triple<>(idMap.get(triple.x()), idMap.get(triple.y()), triple.z()))
                .collect(Collectors.toList());
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
        return this.edgeSet.contains(new Pair<>(i, j));
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

// TODO: Make method for finding the number of connected components in the graph

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
            GraphReader gr = new GraphReader("../datasets/SF.cedge", true);
            gr.printSummary();
//            System.out.println("The matrix:");
//            System.out.println(gr.getAdjacencyMatrix(false));
        } catch (ParseException e) {
            System.out.println("Encountered parsing error at offset " + e.getErrorOffset());
            e.printStackTrace();
            return;
        }
    }
}
