package taxicomp;


import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.flow.EdmondsKarpMFImpl;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;


import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Example {
    //https://jgrapht.org/guide/UserOverview

    //Note: don't try to compile it from maven...
    //directly run using the "RUN" button from left <
    public static void main(String[] args) {

        SimpleDirectedWeightedGraph<Node, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        //understand the correct type to choose for Vertex and Edges.
        //https://jgrapht.org/guide/VertexAndEdgeTypes
        //Then read overview
        //https://jgrapht.org/guide/UserOverview
        Node [] vertexes = {new Node(0),new Node(1),new Node(2),new Node(3)};
        graph.addVertex(vertexes[0]);
        graph.addVertex(vertexes[1]);
        graph.addVertex(vertexes[2]);
        graph.addVertex(vertexes[3]);

        //get all the vertexes by vertexSet method
        Set<Node> vertexSet = graph.vertexSet();
        System.out.println("All the vertexes");
        System.out.println(vertexSet.toString());
        System.out.println();
        //add all edges
        graph.addEdge(vertexes[0], vertexes[1]);
        graph.addEdge(vertexes[0], vertexes[2]);
        graph.addEdge(vertexes[1], vertexes[2]);
        graph.addEdge(vertexes[2], vertexes[1]);
        graph.addEdge(vertexes[1], vertexes[3]);
        graph.addEdge(vertexes[2], vertexes[3]);

        //set edge weight
        graph.setEdgeWeight(graph.getEdge(vertexes[0], vertexes[1]), 2);
        graph.setEdgeWeight(graph.getEdge(vertexes[0], vertexes[2]), 4);
        graph.setEdgeWeight(graph.getEdge(vertexes[1], vertexes[2]), 3);
        graph.setEdgeWeight(graph.getEdge(vertexes[2], vertexes[1]), 3);
        graph.setEdgeWeight(graph.getEdge(vertexes[1], vertexes[3]), 1);
        graph.setEdgeWeight(graph.getEdge(vertexes[2], vertexes[3]), 5);

        // computes all the strongly connected components of the directed graph
        StrongConnectivityAlgorithm<Node, DefaultWeightedEdge> scAlg =
                new KosarajuStrongConnectivityInspector<>(graph);
        List<Graph<Node, DefaultWeightedEdge>> stronglyConnectedSubgraphs =
                scAlg.getStronglyConnectedComponents();

        // prints the strongly connected components
        System.out.println("Strongly connected components:");
        for (int i = 0; i < stronglyConnectedSubgraphs.size(); i++) {
            System.out.println(stronglyConnectedSubgraphs.get(i));
        }
        System.out.println();

        //DijkstraShortestPath from Node 0 to Node 3
        System.out.println("Shortest path from Node 0 to Node 3:");
        DijkstraShortestPath<Node, DefaultWeightedEdge> dijkstraAlg =
                new DijkstraShortestPath<>(graph);
        ShortestPathAlgorithm.SingleSourcePaths<Node, DefaultWeightedEdge> iPaths = dijkstraAlg.getPaths(vertexes[0]);
        System.out.println(iPaths.getPath(vertexes[3]) + "\n");

        //calculate max flow from Node 0 to Node 3
        EdmondsKarpMFImpl<Node, DefaultWeightedEdge> ek = new EdmondsKarpMFImpl<Node, DefaultWeightedEdge>(graph);
        double max_flow = ek.calculateMaximumFlow(vertexes[0], vertexes[3]);
        System.out.println("Max flow from Node 0 to Node 3:");
        System.out.println(max_flow+"\n");



        System.out.println("Find from node 0 from VertexSet");
        Node start = graph
                .vertexSet().stream().filter(node -> node.getID()==0).findAny()
                .get();


        // Depth first search with starting index = 0
        System.out.println("\nDFS from Node 0");
        Iterator<Node> DFSiterator = new DepthFirstIterator<>(graph, start);
        while (DFSiterator.hasNext()) {
            Node node = DFSiterator.next();
            System.out.println(node);
        }

        System.out.println("\nBFS from Node 0");
        Iterator<Node> BFSiterator = new BreadthFirstIterator<>(graph, start);
        while (BFSiterator.hasNext()) {
            Node node = BFSiterator.next();
            System.out.println(node);
        }

    }
}
