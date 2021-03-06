package org.mlarocca.graph;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class ConcurrentGraphTest {
    private static final double PRECISION = 1e-9;

    private static final Random rnd = new Random();

    private static final Set<ConcurrentVertex<String>> testVertices = IntStream.rangeClosed(0, 12)
        .mapToObj(i -> new ConcurrentVertex<>("test" + i, i))
        .collect(Collectors.toSet());

    private static Graph<String> ShortestPathTestGraph = shortestPathGraph();

    ConcurrentGraph<String> graph;

    @Before
    public void setUp() throws Exception {
        graph = new ConcurrentGraph<>();
    }

    @Test
    public void constructor() {
        Double[] v1 = {1., 2., 3.14159};
        Double[] v2 = {-1.2};
        Double[] v3 = {.0, -Math.E};

        List<Double[]> labels = Arrays.asList(v1, v2, v3);
        Set<Vertex<Double[]>> vertices = labels
                .stream()
                .map(label -> new ConcurrentVertex<>(label))
                .collect(Collectors.toSet());
        Set<Edge<Double[]>> edges = new HashSet<>(Arrays.asList(
                new ConcurrentEdge<>(v1, v2),
                new ConcurrentEdge<>(v1, v3),
                new ConcurrentEdge<>(v2, v3)
        ));
        ConcurrentGraph<Double[]> g = new ConcurrentGraph<>(vertices, edges);

        Collection<Vertex<Double[]>> graphVertices = g.getVertices();
        Collection<Edge<Double[]>> graphEdges = g.getEdges();

        assertEquals(vertices.size(), graphVertices.size());
        assertEquals(edges.size(), graphEdges.size());

        for (Vertex<Double[]> vertex : graphVertices) {
            assertTrue(vertices.contains(vertex));
        }

        for (Edge<Double[]> edge : graphEdges) {
            assertTrue(edges.contains(edge));
        }

        g.deleteEdge(v1, v3);
        graphVertices = g.getVertices();
        graphEdges = g.getEdges();

        assertEquals(vertices.size(), graphVertices.size());
        assertEquals(edges.size() - 1, graphEdges.size());

        for (Edge<Double[]> edge : graphEdges) {
            if (!edge.getSource().equals(v1) && !edge.getDestination().equals(v2)) {
                assertTrue(edges.contains(edge));
            }
        }
    }

    @Test
    public void addVertex() throws Exception {
        String label1 = "test";
        assertFalse(graph.hasVertex(label1));
        graph.addVertex(label1);
        assertTrue(graph.hasVertex(label1));
        assertEquals(label1, graph.getVertex(label1).get().getLabel());
        assertEquals(0, graph.getVertex(label1).get().getWeight(), PRECISION);

        String label2 = "second test";
        double weight2 = -3.1415;

        graph.addVertex(label2, weight2);
        assertTrue(graph.hasVertex(label2));
        assertEquals(label2, graph.getVertex(label2).get().getLabel());
        assertEquals(weight2, graph.getVertex(label2).get().getWeight(), PRECISION);
        assertTrue(graph.hasVertex(label1));
        assertEquals(label1, graph.getVertex(label1).get().getLabel());
        assertEquals(0, graph.getVertex(label1).get().getWeight(), PRECISION);

        for (Vertex<String> v: testVertices) {
            graph.addVertex(v.getLabel(), v.getWeight());
        }

        for (Vertex<String> v: testVertices) {
            String label = v.getLabel();
            assertTrue(graph.hasVertex(label));
            assertEquals(label, graph.getVertex(label).get().getLabel());
            assertEquals(v.getWeight(), graph.getVertex(label).get().getWeight(), PRECISION);
        }
    }

    @Test
    public void deleteVertex() throws Exception {
        for (Vertex<String> v: testVertices) {
            graph.addVertex(v.getLabel(), v.getWeight());
        }

        String deletedLabel1 = getRandomCollectionElement(testVertices).getLabel();

        graph.deleteVertex(deletedLabel1);
        assertFalse(graph.hasVertex(deletedLabel1));

        for (Vertex<String> v: testVertices) {
            String label = v.getLabel();
            if (!label.equals(deletedLabel1)) {
                assertTrue(graph.hasVertex(label));
                assertEquals(label, graph.getVertex(label).get().getLabel());
                assertEquals(v.getWeight(), graph.getVertex(label).get().getWeight(), PRECISION);
            }
        }

        String deletedLabel2;
        do {
            deletedLabel2 = getRandomCollectionElement(testVertices).getLabel();
        } while (deletedLabel1.equals(deletedLabel2));
        graph.deleteVertex(deletedLabel2);

        assertFalse(graph.hasVertex(deletedLabel1));
        assertFalse(graph.hasVertex(deletedLabel2));

        for (Vertex<String> v: testVertices) {
            String label = v.getLabel();
            if (!label.equals(deletedLabel1) && !label.equals(deletedLabel2)) {
                assertTrue(graph.hasVertex(label));
                assertEquals(label, graph.getVertex(label).get().getLabel());
                assertEquals(v.getWeight(), graph.getVertex(label).get().getWeight(), PRECISION);
            }
        }
    }

    @Test
    public void getVertex() throws Exception {
        String label = "xyz1";
        assertEquals(Optional.empty(), graph.getVertex(label));
        graph.addVertex(label);
        assertNotEquals(Optional.empty(), graph.getVertex(label));
        assertEquals(label, graph.getVertex(label).get().getLabel());
    }

    @Test
    public void addEdge() throws Exception {

    }

    @Test
    public void getEdge() throws Exception {

    }

    @Test
    public void deleteEdge() throws Exception {

    }

    @Test
    public void getEdges() throws Exception {

    }

    @Test
    public void getEdgesFrom() throws Exception {

    }

    @Test
    public void getEdgesTo() throws Exception {

    }

    // MULTI-THREADING

    @Test
    public void testMultiThreading() {
        int maxWait = 25;
        int numEdgeReaders = 3;
        int numVertexReaders = 4;
        int readIterations = 10;
        int maxVertices = 50;

        ExecutorService executor = Executors.newFixedThreadPool(10);

        Integer v1 = 1;
        Integer v2 = 2;
        Integer v3 = 3;
        Integer v4 = 4;
        Integer v5 = 5;
        Integer v6 = 6;
        Integer v7 = 7;
        Integer v8 = 8;

        Graph<Integer> g = new ConcurrentGraph<>(Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8));

        final List<Edge<Integer>> edges = new ArrayList<>();
        final List<Vertex<Integer>> vertices = new ArrayList<>();

        IntStream.range(1, 4).forEach(i -> {
            try {
                g.addEdge(i, (i+1));
                Thread.sleep(1 + rnd.nextInt(maxWait));
            } catch (NoSuchElementException | InterruptedException e) {
            }
        });

        Runnable vertexAdder = () -> {
            IntStream.rangeClosed(9, maxVertices).forEach(i -> {
                try {
                    g.addVertex(i);
                    Thread.sleep(1 + rnd.nextInt(maxWait));
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });
        };

        Runnable vertexRemover = () -> {
            IntStream.rangeClosed(5, 8).forEach(i -> {
                try {
                    g.deleteVertex(i);
                    Thread.sleep(1 + rnd.nextInt(maxWait));
                } catch (InterruptedException e) {
                }
            });
        };

        Runnable edgeAdder = () -> {
            IntStream.range(1, 4).forEach(i -> {
                try {
                    g.addEdge(i, (i+1));
                    Thread.sleep(1 + rnd.nextInt(maxWait));
                } catch (NoSuchElementException | InterruptedException e) {
                }
            });
        };

        Runnable edgeReader = () -> {
            try {
                Thread.sleep(1);
                IntStream.range(0, readIterations).forEach(j ->
                    IntStream.range(1, 4).forEach(i -> {
                        try {
                            edges.add(g.getEdge(i, (i+1)).get());
                            Thread.sleep(rnd.nextInt(maxWait));
                        } catch (InterruptedException e) {
                        }
                    }));
            } catch (InterruptedException e) {
            }
        };

        Runnable vertexReader = () -> {
            try {
                Thread.sleep(1);
                IntStream.range(0, readIterations).forEach(j ->
                        IntStream.rangeClosed(1, 4).forEach(i -> {
                            try {
                                vertices.add(g.getVertex(i).get());
                                Thread.sleep(rnd.nextInt(maxWait));
                            } catch (InterruptedException e) {
                            }
                        }));
            } catch (InterruptedException e) {
            }
        };

        executor.execute(vertexAdder);
        executor.execute(edgeAdder);
        executor.execute(vertexRemover);

        IntStream.range(0, numEdgeReaders)
                .forEach(i -> executor.execute(edgeReader));
        IntStream.range(0, numVertexReaders)
                .forEach(i -> executor.execute(vertexReader));

        try {
            executor.awaitTermination((maxVertices + readIterations) * (1 + maxWait), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new AssertionError("Computation was stuck");
        };

        // 8 initial, 42 added, 4 removed
        assertEquals(46, g.getVertices().size());
        assertEquals(3, g.getEdges().size());

        assertEquals(numEdgeReaders * 3 * readIterations, edges.size());
        assertEquals(numVertexReaders * 4 * readIterations, vertices.size());

        List<Integer> verticesLabels = vertices
                .stream()
                .map(Vertex::getLabel)
                .collect(Collectors.toList());
        Map<Integer, Long> verticesCount = verticesLabels
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        IntStream.rangeClosed(1, 4).forEach(i ->
            assertEquals(verticesCount.get(i), numVertexReaders * readIterations, PRECISION));
    }


    // ADVANCED ALGORITHMS


    @Test
    public void topologicalSortOnEmptyGraph() throws Exception {
        ConcurrentGraph<String> g = new ConcurrentGraph<>();
        assertEquals(Collections.EMPTY_LIST, g.topologicalSort());
    }

    @Test
    public void topologicalSortOnAcyclicGraph() throws Exception {
        String v1 = "A";
        String v2 = "B";
        String v3 = "C";
        String v4 = "D";
        String v5 = "E";

        List<String> labels = Arrays.asList(v1, v2, v3, v4, v5);
        Set<Vertex<String>> vertices = labels
                .stream()
                .map(label -> new ConcurrentVertex<>(label))
                .collect(Collectors.toSet());

        Set<Edge<String>> edges = new HashSet<>(Arrays.asList(
                new ConcurrentEdge<>(v5, v2),
                new ConcurrentEdge<>(v5, v3),
                new ConcurrentEdge<>(v2, v4),
                new ConcurrentEdge<>(v3, v4),
                new ConcurrentEdge<>(v4, v1)
        ));
        ConcurrentGraph<String> g = new ConcurrentGraph<>(vertices, edges);

        Set<List<Vertex<String>>> possibleSolutions = new HashSet<List<Vertex<String>>>(Arrays.asList(
                Arrays.asList(v5, v2, v3, v4 ,v1).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v5, v3, v2, v4 ,v1).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList())
        ));

        List<Vertex<String>> topologicalSort = g.topologicalSort();

        assertTrue(possibleSolutions.contains(topologicalSort));
    }

    public void topologicalSortOnCyclicGraph() throws Exception {
        String v1 = "A";
        String v2 = "B";
        String v3 = "C";

        List<String> labels = Arrays.asList(v1, v2, v3);
        Set<Vertex<String>> vertices = labels
                .stream()
                .map(label -> new ConcurrentVertex<>(label))
                .collect(Collectors.toSet());

        Set<Edge<String>> edges = new HashSet<>(Arrays.asList(
                new ConcurrentEdge<>(v1, v3),
                new ConcurrentEdge<>(v3, v3),
                new ConcurrentEdge<>(v3, v1)
        ));
        ConcurrentGraph<String> g = new ConcurrentGraph<>(vertices, edges);

        Set<List<Vertex<String>>> possibleSolutions = new HashSet<List<Vertex<String>>>(Arrays.asList(
                Arrays.asList(v1, v2, v3).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v2, v3, v1).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v3, v1, v2).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList())
        ));

        List<Vertex<String>> topologicalSort = g.topologicalSort();

        assertTrue(possibleSolutions.contains(topologicalSort));
    }

    public void topologicalSortOnDisconnectedGraph() throws Exception {
        String v1 = "A1";
        String v2 = "B2";
        String v3 = "C3";
        String v4 = "D4";
        String v5 = "E5";

        List<String> labels = Arrays.asList(v1, v2, v3, v4, v5);
        Set<Vertex<String>> vertices = labels
                .stream()
                .map(label -> new ConcurrentVertex<>(label))
                .collect(Collectors.toSet());

        Set<Edge<String>> edges = new HashSet<>(Arrays.asList(
                new ConcurrentEdge<>(v1, v3),
                new ConcurrentEdge<>(v3, v3),
                new ConcurrentEdge<>(v3, v1),
                new ConcurrentEdge<>(v4, v5)
        ));
        ConcurrentGraph<String> g = new ConcurrentGraph<>(vertices, edges);

        Set<List<Vertex<String>>> possibleSolutions = new HashSet<>(Arrays.asList(
                Arrays.asList(v1, v2, v3, v4, v5).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v2, v3, v1, v4, v5).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v3, v1, v2, v4, v5).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v4, v5, v1, v2, v3).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v4, v5, v2, v3, v1).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList()),
                Arrays.asList(v4, v5, v3, v1, v2).stream().map(v -> g.getVertex(v).get()).collect(Collectors.toList())
        ));

        List<Vertex<String>> topologicalSort = g.topologicalSort();

        assertTrue(possibleSolutions.contains(topologicalSort));
    }

    @Test
    public void isAcyclic() throws Exception {

    }

    @Test
    public void isConnected() throws Exception {

    }

    @Test
    public void isStronglyConnected() throws Exception {

    }

    @Test
    public void transpose() throws Exception {
        String v1 = "A1";
        String v2 = "B2";
        String v3 = "C3";
        String v4 = "D4";
        String v5 = "E5";

        List<String> labels = Arrays.asList(v1, v2, v3, v4, v5);
        Set<Vertex<String>> vertices = labels
                .stream()
                .map(label -> new ConcurrentVertex<>(label))
                .collect(Collectors.toSet());

        Set<Edge<String>> edges = new HashSet<>(Arrays.asList(
                new ConcurrentEdge<>(v1, v3),
                new ConcurrentEdge<>(v3, v3),
                new ConcurrentEdge<>(v3, v1),
                new ConcurrentEdge<>(v4, v5)
        ));
        ConcurrentGraph<String> g = new ConcurrentGraph<>(vertices, edges);
        ConcurrentGraph<String> gT = g.transpose();

        assertEquals(g.getVertices().size(), gT.getVertices().size());
        assertEquals(g.getEdges().size(), gT.getEdges().size());

        g.getVertices().forEach(v -> {
            assertTrue(gT.hasVertex(v.getLabel()));
        });

        g.getEdges().forEach(e -> {
            assertTrue(gT.getEdge(e.getDestination(), e.getSource()).isPresent());
        });
    }

    @Test
    public void BFSAllDestinations() throws Exception {
        String source = "S";
        // Just a shorter alias
        Graph<String> g = ShortestPathTestGraph;
        Map<Vertex<String>, GraphSearchResult<String>> allPaths = ShortestPathTestGraph.BFS(source);
        GraphSearchResult<String> result;
        List<Edge<String>> expectedPath;
        Set<List<Edge<String>>> possibleSolutions;

        // Source should have a 0-length path to itself, with 0 distance
        result = allPaths.get(g.getVertex(source).get());
        assertEquals(0, result.distance(), PRECISION);
        assertEquals(0, result.path().get().size());

        // A disconnected node should have empty path, with infinite distance
        result = allPaths.get(g.getVertex("disconnected").get());
        assertEquals(Double.POSITIVE_INFINITY, result.distance(), PRECISION);
        assertEquals(Optional.empty(), result.path());

        // Other vertices
        result = allPaths.get(g.getVertex("a").get());
        assertEquals(1, result.distance(), PRECISION);
        expectedPath = Arrays.asList(g.getEdge(source, "a").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("b").get());
        assertEquals(1, result.distance(), PRECISION);
        expectedPath = Arrays.asList(g.getEdge(source, "b").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("c").get());
        assertEquals(2, result.distance(), PRECISION);
        possibleSolutions = new HashSet<>(Arrays.asList(
                Arrays.asList(
                        g.getEdge(source, "a").get(),
                        g.getEdge("a", "c").get()),
                Arrays.asList(
                        g.getEdge(source, "b").get(),
                        g.getEdge("b", "c").get())));

        assertTrue(possibleSolutions.contains(result.path().get()));

        result = allPaths.get(g.getVertex("d").get());
        assertEquals(2, result.distance(), PRECISION);
        expectedPath = Arrays.asList(
                g.getEdge(source, "b").get(),
                g.getEdge("b", "d").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("e").get());
        assertEquals(3, result.distance(), PRECISION);
        possibleSolutions = new HashSet<>(Arrays.asList(
                Arrays.asList(
                    g.getEdge(source, "a").get(),
                    g.getEdge("a", "c").get(),
                    g.getEdge("c", "e").get()),
                Arrays.asList(
                        g.getEdge(source, "b").get(),
                        g.getEdge("b", "d").get(),
                        g.getEdge("d", "e").get())));
        assertTrue(possibleSolutions.contains(result.path().get()));
    }

    @Test
    public void BFSSingleDestination() throws Exception {
        String source = "S";
        String dest = "e";
        // Just a shorter alias for the graph
        Graph<String> g = ShortestPathTestGraph;
        GraphSearchResult<String> result = ShortestPathTestGraph.BFS(source, dest);

        assertEquals(3, result.distance(), PRECISION);
        Set<List<Edge<String>>> possibleSolutions = new HashSet<>(Arrays.asList(
                Arrays.asList(
                        g.getEdge(source, "a").get(),
                        g.getEdge("a", "c").get(),
                        g.getEdge("c", "e").get()),
                Arrays.asList(
                        g.getEdge(source, "b").get(),
                        g.getEdge("b", "d").get(),
                        g.getEdge("d", "e").get())));
        assertTrue(possibleSolutions.contains(result.path().get()));
    }

    @Test
    public void dijkstraAllDestinations() throws Exception {
        String source = "S";
        // Just a shorter alias for the graph
        Graph<String> g = ShortestPathTestGraph;
        Map<Vertex<String>, GraphSearchResult<String>> allPaths = ShortestPathTestGraph.Dijkstra(source);
        GraphSearchResult<String> result;
        List<Edge<String>> expectedPath;

        // Source should have a 0-length path to itself, with 0 distance
        result = allPaths.get(g.getVertex(source).get());
        assertEquals(0, result.distance(), PRECISION);
        assertEquals(0, result.path().get().size());

        // A disconnected node should have empty path, with infinite distance
        result = allPaths.get(g.getVertex("disconnected").get());
        assertEquals(Double.POSITIVE_INFINITY, result.distance(), PRECISION);
        assertEquals(Optional.empty(), result.path());

        // Other vertices
        result = allPaths.get(g.getVertex("a").get());
        assertEquals(10, result.distance(), PRECISION);
        expectedPath = Arrays.asList(g.getEdge(source, "a").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("b").get());
        assertEquals(3, result.distance(), PRECISION);
        expectedPath = Arrays.asList(g.getEdge(source, "b").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("c").get());
        assertEquals(12, result.distance(), PRECISION);
        expectedPath = Arrays.asList(
                        g.getEdge(source, "a").get(),
                        g.getEdge("a", "c").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("d").get());
        assertEquals(16, result.distance(), PRECISION);
        expectedPath = Arrays.asList(
                g.getEdge(source, "a").get(),
                g.getEdge("a", "c").get(),
                g.getEdge("c", "d").get());
        assertEquals(expectedPath, result.path().get());

        result = allPaths.get(g.getVertex("e").get());
        assertEquals(21, result.distance(), PRECISION);
        expectedPath = Arrays.asList(
                g.getEdge(source, "a").get(),
                g.getEdge("a", "c").get(),
                g.getEdge("c", "d").get(),
                g.getEdge("d", "e").get());
        assertEquals(expectedPath, result.path().get());
    }

    @Test
    public void dijkstraSingleDestination() throws Exception {
        String source = "S";
        String dest = "e";
        // Just a shorter alias for the graph
        Graph<String> g = ShortestPathTestGraph;
        GraphSearchResult<String> result = ShortestPathTestGraph.Dijkstra(source, dest);

        assertEquals(21, result.distance(), PRECISION);
        List<Edge<String>> expectedPath = Arrays.asList(
                g.getEdge(source, "a").get(),
                g.getEdge("a", "c").get(),
                g.getEdge("c", "d").get(),
                g.getEdge("d", "e").get());
        assertEquals(expectedPath, result.path().get());
    }

    @Test
    public void stronglyConnectedComponents() throws Exception {
        Graph<String> g = sccGraph();
        Set<Set<Vertex<String>>> sccs = g.stronglyConnectedComponents();
        assertEquals(3, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(
                g.getVertex("g").get(),
                g.getVertex("h").get(),
                g.getVertex("i").get()
        ))));
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(
                g.getVertex("a").get(),
                g.getVertex("e").get(),
                g.getVertex("f").get()
        ))));
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(
                g.getVertex("b").get(),
                g.getVertex("c").get(),
                g.getVertex("d").get()
        ))));
    }


    private static <T> T getRandomCollectionElement(Collection<T> collection) {
        int n = collection.size();
        Iterator<T> iterator = collection.iterator();
        int k = rnd.nextInt(n);
        for (int i = 0; i < k; i++) {
            iterator.next();
        }
        return iterator.next();
    }

    private static Graph<String> shortestPathGraph() {
        String vs = "S";
        String va = "a";
        String vb = "b";
        String vc = "c";
        String vd = "d";
        String ve = "e";
        String vDisco = "disconnected";

        List<String> labels = Arrays.asList(vs, va, vb, vc, vd, ve, vDisco);

        Graph<String> g = new ConcurrentGraph<>(labels);

        g.addEdge(vs, va, 10);
        g.addEdge(vs, vb, 3);
        g.addEdge(va, vc, 2);
        g.addEdge(vb, vc, 15);
        g.addEdge(vc, vb, 1);
        g.addEdge(vc, ve, 42);
        g.addEdge(vb, vd, 30);
        g.addEdge(vc, vd, 4);
        g.addEdge(vd, vc, 5);
        g.addEdge(vd, ve, 5);

        return g;
    }

    private static Graph<String> sccGraph() {
        String v1 = "a";
        String v2 = "b";
        String v3 = "c";
        String v4 = "d";
        String v5 = "e";
        String v6 = "f";
        String v7 = "g";
        String v8 = "h";
        String v9 = "i";

        List<String> labels = Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9);

        Graph<String> g = new ConcurrentGraph<>(labels);

        g.addEdge(v1, v5);
        g.addEdge(v2, v3);
        g.addEdge(v3, v4);
        g.addEdge(v4, v2);
        g.addEdge(v4, v5);
        g.addEdge(v5, v6);
        g.addEdge(v6, v1);
        g.addEdge(v6, v9);
        g.addEdge(v7, v8);
        g.addEdge(v8, v9);
        g.addEdge(v9, v7);

        return g;
    }
}