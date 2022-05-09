package graphReader;

import APSPSolver.APSPSolver;
import APSPSolver.SerialDijkstra;
import APSPSolver.MatSquare;
import javafx.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class GraphCompressorTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.INFO);
    }

    @Test
    void graphCompressorCorrectlySolvesAPSPOnSmallExampleGraph1() {
        // The graph in this test looks like this:
        //       13        7
        //  6 ------- 7 ------- 8
        //  |11    /            |4
        //  5   /10             9
        // 5| /                 |3
        //  4 ------- 3 ------- 2 ---- 1 ---- 0
        //       6        5         2      1

        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/compressor-test1.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The graph file is not present");
            return;
        }
        GraphCompressor solver = new GraphCompressor(graphReader, GraphCompressor.getCurriedFoxOttoAPSPSolverConstructor(4));

        // ACT
        solver.solve();

        // ASSERT

        // size of compressed graph is 4 edges
        assertEquals(4, solver.getCompressedGraph().getNumberOfNodes(), "The compressed graph has 4 nodes");
        // 0 -> 2
        assertEquals(Optional.of(Arrays.asList(0, 1, 2)), solver.getShortestPath(0, 2));
        assertEquals(3.0, solver.getDistanceFrom(0, 2));
        // 0 -> 5
        assertEquals(Optional.of(Arrays.asList(0, 1, 2, 3, 4, 5)), solver.getShortestPath(0, 5));
        assertEquals(19.0, solver.getDistanceFrom(0, 5));
        // 7 -> 3
        assertEquals(Optional.of(Arrays.asList(7, 4, 3)), solver.getShortestPath(7, 3));
        assertEquals(16.0, solver.getDistanceFrom(7, 3));
    }

    @Test
    void graphCompressorCorrectlySolvesAPSPOnSmallExampleGraph2() {
        // The graph in this test looks like this:
        //   1        2       5       6
        // 0 ---- 1 ----- 2 ----- 3 ---- 4
        //      ____3___/                |
        //    /                          |   5
        // 9 ---- 8 ----- 7 ----- 6 ---- 5
        //    4      7       13      11

        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/compressor-test2.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The graph file is not present");
            return;
        }
        GraphCompressor solver = new GraphCompressor(graphReader, GraphCompressor.getCurriedFoxOttoAPSPSolverConstructor(4));

        // ACT
        solver.solve();

        // ASSERT

        // size of compressed graph is 3 edges (we have the "Q" edge-case, so one 2-degree vertex is saved
        assertEquals(3, solver.getCompressedGraph().getNumberOfNodes(), "The compressed graph has 3 nodes");
        // 0 -> 2
        assertEquals(Optional.of(Arrays.asList(0, 1, 2)), solver.getShortestPath(0, 2));
        assertEquals(3.0, solver.getDistanceFrom(0, 2));
        // 9 -> 3
        assertEquals(Optional.of(Arrays.asList(9, 2, 3)), solver.getShortestPath(9, 3));
        assertEquals(8.0, solver.getDistanceFrom(9, 3));
        // 4 -> 6
        assertEquals(Optional.of(Arrays.asList(4, 5, 6)), solver.getShortestPath(4, 6));
        assertEquals(16.0, solver.getDistanceFrom(4, 6));
        // 7 -> 2
        assertEquals(Optional.of(Arrays.asList(7, 8, 9, 2)), solver.getShortestPath(7, 2));
        assertEquals(14.0, solver.getDistanceFrom(7, 2));
    }

    @Test
    void graphCompressorCorrectlySolvesAPSPOnLargeGraph() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/OL-but-smaller.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The graph file could not be read");
            return;
        }
        GraphCompressor compressSolver = new GraphCompressor(graphReader, GraphCompressor.getCurriedFoxOttoAPSPSolverConstructor(4));
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        // ACT
        compressSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        List<List<Pair<Integer, Double>>> adjList = graphReader.getAdjacencyList();
        for (int i = 0; i < graphReader.getNumberOfNodes(); i++){
            for (int j = 0; j < graphReader.getNumberOfNodes(); j++) {
                LOGGER.info("==== TEST: Finding shortest path " + i + " --> " + j);
                if (!dijkstraSolver.getShortestPath(i, j).equals(compressSolver.getShortestPath(i, j))) {
                    for (int n : compressSolver.getShortestPath(i, j).orElseGet(Collections::emptyList)) {
                        LOGGER.info("Neighbours of node " + n + ": " + adjList.get(n));
                    }
                }
                assertEquals(dijkstraSolver.getShortestPath(i, j), compressSolver.getShortestPath(i, j),
                        "The reconstructed path is correct");
                assertEquals(dijkstraSolver.getDistanceFrom(i, j).doubleValue(), compressSolver.getDistanceFrom(i, j).doubleValue(),
                        1E-7, "The distance between the nodes is correct");
            }
        }
    }
}