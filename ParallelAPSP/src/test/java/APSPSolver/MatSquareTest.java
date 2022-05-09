package APSPSolver;

import graphReader.GraphCompressor;
import graphReader.GraphReader;
import matrixMultiplication.FoxOtto;
import matrixMultiplication.GeneralisedFoxOtto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.LoggerFormatter;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MatSquareTest {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @BeforeAll
    static void setupLogger() {
        LoggerFormatter.setupLogger(LOGGER, Level.INFO);
    }

    @Test
    void nonGeneralAPSPAlgorithmGivesCorrectResultOnSmallGraph1() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/7-node-example.cedge", true);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The test data could not be read");
            return;
        }
        // fox otto solver
        APSPSolver matrixSolver = new MatSquare(graphReader, FoxOtto.class);
        // dijkstra solver
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        // ACT
        matrixSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                // expected is first item, actual is second item
                assertEquals(dijkstraSolver.getDistanceFrom(i, j),
                        matrixSolver.getDistanceFrom(i, j), "The distance from node " + i + " to node " + j
                         + " is correct");
                assertEquals(dijkstraSolver.getShortestPath(i, j), matrixSolver.getShortestPath(i, j),
                        "The shortest path produced is correct: " + i + " -> " + j + " with dist="
                         + matrixSolver.getDistanceFrom(i, j));
            }
        }
    }

    @Test
    void nonGeneralAPSPAlgorithmGivesCorrectResultOnSmallGraph2() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/9-node-example.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The test data could not be read");
            return;
        }
        // fox otto solver
        APSPSolver matrixSolver = new MatSquare(graphReader, FoxOtto.class);
        // dijkstra solver
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        // ACT
        matrixSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                // expected is first item, actual is second item
                assertEquals(dijkstraSolver.getDistanceFrom(i, j),
                        matrixSolver.getDistanceFrom(i, j), "The distance from node " + i + " to node " + j
                                + " is correct");
                assertEquals(dijkstraSolver.getShortestPath(i, j), matrixSolver.getShortestPath(i, j),
                        "The shortest path produced is correct: " + i + " -> " + j + " with dist="
                                + matrixSolver.getDistanceFrom(i, j));
            }
        }
    }


    @Test
    void nonGeneralAPSPAlgorithmGivesCorrectResultOnLargeGraph() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/OL-but-smaller.cedge", false);
            // compress it
            GraphCompressor graphCompressor = new GraphCompressor(graphReader);
            graphReader = graphCompressor.getCompressedGraph();
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The test data could not be read");
            return;
        }
        // fox otto solver
        APSPSolver matrixSolver = new MatSquare(graphReader, FoxOtto.class);
        // dijkstra solver
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        int num_nodes = graphReader.getNumberOfNodes();
        System.out.println("Testing correctness on graph with " + num_nodes + " nodes.");

        // ACT
        matrixSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        for (int i = 0; i < num_nodes; i++) {
            for (int j = 0; j < num_nodes; j++) {
                // expected is first item, actual is second item
                assertEquals(dijkstraSolver.getDistanceFrom(i, j).doubleValue(),
                        matrixSolver.getDistanceFrom(i, j).doubleValue(), 1E-5, "The distance from node " + i + " to node " + j
                                + " is correct");
                assertEquals(dijkstraSolver.getShortestPath(i, j), matrixSolver.getShortestPath(i, j),
                        "The shortest path produced is correct: " + i + " -> " + j + " with dist="
                                + matrixSolver.getDistanceFrom(i, j));
            }
        }
    }

    @Test
    void generalisedAPSPAlgorithmGivesCorrectResultOnLargeGraph() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/OL-but-smaller.cedge", false);
            // compress it
            GraphCompressor graphCompressor = new GraphCompressor(graphReader);
            graphReader = graphCompressor.getCompressedGraph();
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The test data could not be read");
            return;
        }
        // general fox otto solver with 8 x 8 processing elements
        APSPSolver matrixSolver = new MatSquare(graphReader, 8, GeneralisedFoxOtto.class);
        // dijkstra solver
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        int num_nodes = graphReader.getNumberOfNodes();
        System.out.println("Testing correctness on graph with " + num_nodes + " nodes.");

        // ACT
        matrixSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        for (int i = 0; i < num_nodes; i++) {
            for (int j = 0; j < num_nodes; j++) {
                // expected is first item, actual is second item
                assertEquals(dijkstraSolver.getDistanceFrom(i, j).doubleValue(),
                        matrixSolver.getDistanceFrom(i, j).doubleValue(), 1E-5, "The distance from node " + i + " to node " + j
                                + " is correct");
                assertEquals(dijkstraSolver.getShortestPath(i, j), matrixSolver.getShortestPath(i, j),
                        "The shortest path produced is correct: " + i + " -> " + j + " with dist="
                                + matrixSolver.getDistanceFrom(i, j));
            }
        }
    }

    // we now test the generalised version

    @Test
    void generalisedAPSPAlgorithmGivesCorrectResultOnSmallGraph1() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/7-node-example.cedge", true);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The test data could not be read");
            return;
        }
        // fox otto solver
        APSPSolver matrixSolver = new MatSquare(graphReader,  2, GeneralisedFoxOtto.class);
        // dijkstra solver
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        // ACT
        matrixSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                // expected is first item, actual is second item
                assertEquals(dijkstraSolver.getDistanceFrom(i, j),
                        matrixSolver.getDistanceFrom(i, j), "The distance from node " + i + " to node " + j
                                + " is correct");
                assertEquals(dijkstraSolver.getShortestPath(i, j), matrixSolver.getShortestPath(i, j),
                        "The shortest path produced is correct: " + i + " -> " + j + " with dist="
                                + matrixSolver.getDistanceFrom(i, j));
            }
        }
    }

    @Test
    void generalisedAPSPAlgorithmGivesCorrectResultOnSmallGraph2() {
        // SETUP
        GraphReader graphReader;
        try {
            graphReader = new GraphReader("../test-datasets/9-node-example.cedge", false);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("The test data could not be read");
            return;
        }
        // fox otto solver
        APSPSolver matrixSolver = new MatSquare(graphReader, 4, GeneralisedFoxOtto.class);
        // dijkstra solver
        APSPSolver dijkstraSolver = new SerialDijkstra(graphReader);

        // ACT
        matrixSolver.solve();
        dijkstraSolver.solve();

        // ASSERT
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                // expected is first item, actual is second item
                assertEquals(dijkstraSolver.getDistanceFrom(i, j),
                        matrixSolver.getDistanceFrom(i, j), "The distance from node " + i + " to node " + j
                                + " is correct");
                assertEquals(dijkstraSolver.getShortestPath(i, j), matrixSolver.getShortestPath(i, j),
                        "The shortest path produced is correct: " + i + " -> " + j + " with dist="
                                + matrixSolver.getDistanceFrom(i, j));
            }
        }
    }
}