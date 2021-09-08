package org.opentripplanner.graph_builder.module;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.common.geometry.Subgraph;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.issues.GraphConnectivity;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.ElevatorEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetTransitEntranceLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * this module is part of the  {@link org.opentripplanner.graph_builder.services.GraphBuilderModule}
 * process. it design to remove small isolated islands form the graph. Islands are created when
 * there is no connectivity in the map, island acts like trap since there is no connectivity there
 * is no way in or out the island. The module distinguish between two island types one with transit
 * stops and one without stops.
 */
public class PruneFloatingIslands implements GraphBuilderModule {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(PruneFloatingIslands.class);

    /**
     * this field indicate the maximum size for island without stops
     * island under this size will be pruned.
     */
    private int pruningThresholdIslandWithoutStops;

    /**
     * this field indicate the maximum size for island with stops
     * island under this size will be pruned.
     */
    private int pruningThresholdIslandWithStops;

    /**
     * The name for output file for this process. The file will store information about the islands
     * that were found and whether they were pruned. If the value is an empty string or null there
     * will be no output file.
     */
    private String islandLogFile;

    private StreetLinkerModule transitToStreetNetwork;

    private static int islandCounter = 0;

    public List<String> provides() {
        return Collections.emptyList();
    }

    public List<String> getPrerequisites() {
        /**this module can run after the street module only but if
         * the street linker did not run then it couldn't identifies island with stops.
         * so if the need is to distinguish between island with stops or without stops
         * as explained before this module should run after the streets and the linker modules.
         */
        return Arrays.asList("streets");
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        LOG.info("Pruning isolated islands in street network");

        pruneFloatingIslands(graph, pruningThresholdIslandWithoutStops,
                pruningThresholdIslandWithStops, islandLogFile, issueStore
        );
        if (transitToStreetNetwork == null) {
            LOG.debug(
                "TransitToStreetNetworkGraphBuilder was not provided to PruneFloatingIslands. Not attempting to reconnect stops.");
        }
        else {
            //reconnect stops on small islands (that removed)
            transitToStreetNetwork.buildGraph(graph, extra, issueStore);
        }
        LOG.debug("Done pruning isolated islands");
    }

    @Override
    public void checkInputs() {
        //no inputs
    }

    public void setPruningThresholdIslandWithoutStops(int pruningThresholdIslandWithoutStops) {
        this.pruningThresholdIslandWithoutStops = pruningThresholdIslandWithoutStops;
    }

    public void setPruningThresholdIslandWithStops(int pruningThresholdIslandWithStops) {
        this.pruningThresholdIslandWithStops = pruningThresholdIslandWithStops;
    }

    private static void pruneFloatingIslands(Graph graph, int maxIslandSize,
            int islandWithStopMaxSize, String islandLogName, DataImportIssueStore issueStore) {
        LOG.debug("pruning");
        PrintWriter islandLog = null;
        if (islandLogName != null && !islandLogName.isEmpty()) {
            try {
                islandLog = new PrintWriter(new File(islandLogName));
            } catch (Exception e) {
                LOG.error("Failed to write islands log file", e);
            }
        }
        if (islandLog != null) {
            islandLog.printf("%s\t%s\t%s\t%s\t%s\n","id","stopCount", "streetCount","wkt" ,"hadRemoved");
        }
        Map<Vertex, Subgraph> subgraphs = new HashMap<Vertex, Subgraph>();
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<Vertex, ArrayList<Vertex>>();

//      RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK, TraverseMode.TRANSIT));
        RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK));

        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            State s0 = new State(gv, options);
            for (Edge e : gv.getOutgoing()) {
                Vertex in = gv;
                if (!(e instanceof StreetEdge || e instanceof StreetTransitStopLink ||
                    e instanceof StreetTransitEntranceLink || e instanceof ElevatorEdge ||
                    e instanceof FreeEdge)
                ) {
                    continue;
                }
                State s1 = e.traverse(s0);
                if (s1 == null) {
                    continue;
                }
                Vertex out = s1.getVertex();

                ArrayList<Vertex> vertexList = neighborsForVertex.get(in);
                if (vertexList == null) {
                    vertexList = new ArrayList<Vertex>();
                    neighborsForVertex.put(in, vertexList);
                }
                vertexList.add(out);

                vertexList = neighborsForVertex.get(out);
                if (vertexList == null) {
                    vertexList = new ArrayList<Vertex>();
                    neighborsForVertex.put(out, vertexList);
                }
                vertexList.add(in);
            }
        }

        ArrayList<Subgraph> islands = new ArrayList<Subgraph>();
        /* associate each node with a subgraph */
        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            Vertex vertex = gv;
            if (subgraphs.containsKey(vertex)) {
                continue;
            }
            if (!neighborsForVertex.containsKey(vertex)) {
                continue;
            }
            Subgraph subgraph = computeConnectedSubgraph(neighborsForVertex, vertex);
            if (subgraph != null){
                for (Iterator<Vertex> vIter = subgraph.streetIterator(); vIter.hasNext();) {
                    Vertex subnode = vIter.next();
                    subgraphs.put(subnode, subgraph);
                }
                islands.add(subgraph);
            }
        }
        LOG.info(islands.size() + " sub graphs found");
        /* remove all tiny subgraphs and large subgraphs without stops */
        for (Subgraph island : islands) {
            boolean hadRemoved = false;
            if(island.stopSize() > 0){
            //for islands with stops
                if (island.streetSize() < islandWithStopMaxSize) {
                    depedestrianizeOrRemove(graph, island, issueStore);
                    hadRemoved = true;
                }
            }else{
            //for islands without stops
                if (island.streetSize() < maxIslandSize) {
                    depedestrianizeOrRemove(graph, island, issueStore);
                    hadRemoved = true;
                }
            }
            if (islandLog != null) {
                WriteNodesInSubGraph(island, islandLog, hadRemoved);
            }
        }
        if (graph.removeEdgelessVertices() > 0) {
            LOG.info("Removed edgeless vertices after pruning islands");
        }
    }

    private static void depedestrianizeOrRemove(
            Graph graph,
            Subgraph island,
            DataImportIssueStore issueStore
    ) {
        //iterate over the street vertex of the subgraph
        for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            Collection<Edge> outgoing = new ArrayList<Edge>(v.getOutgoing());
            for (Edge e : outgoing) {
                if (e instanceof StreetEdge) {
                    StreetEdge pse = (StreetEdge) e;
                    StreetTraversalPermission permission = pse.getPermission();
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                    if (permission == StreetTraversalPermission.NONE) {
                        graph.removeEdge(pse);
                    } else {
                        pse.setPermission(permission);
                    }
                }
            }
        }

        for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            if (v.getDegreeOut() + v.getDegreeIn() == 0) {
                graph.remove(v);
            }
        }
        //remove street conncetion form
        for (Iterator<Vertex> vIter = island.stopIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            Collection<Edge> edges = new ArrayList<Edge>(v.getOutgoing());
            edges.addAll(v.getIncoming());
            for (Edge e : edges) {
                if (e instanceof StreetTransitStopLink || e instanceof StreetTransitEntranceLink) {
                    graph.removeEdge(e);
                }
            }
        }
        issueStore.add(new GraphConnectivity(island.getRepresentativeVertex(), island.streetSize()));
    }

    private static Subgraph computeConnectedSubgraph(
            Map<Vertex, ArrayList<Vertex>> neighborsForVertex, Vertex startVertex) {
        Subgraph subgraph = new Subgraph();
        Queue<Vertex> q = new LinkedList<Vertex>();
        q.add(startVertex);
        while (!q.isEmpty()) {
            Vertex vertex = q.poll();
            for (Vertex neighbor : neighborsForVertex.get(vertex)) {
                if (!subgraph.contains(neighbor)) {
                    subgraph.addVertex(neighbor);
                    q.add(neighbor);
                }
            }
        }
        return subgraph;
//        if(subgraph.size()>1) return subgraph;
//        return null;
    }

    private static void WriteNodesInSubGraph(
            Subgraph subgraph,
            PrintWriter islandLog,
            boolean hadRemoved
    ) {
        Geometry convexHullGeom = subgraph.getConvexHull();
        if (convexHullGeom != null && !(convexHullGeom instanceof Polygon)) {
            convexHullGeom = convexHullGeom.buffer(0.0001, 5);
        }
        islandLog.printf("%d\t%d\t%d\t%s\t%b\n", islandCounter, subgraph.stopSize(),
                subgraph.streetSize(), convexHullGeom, hadRemoved
        );
        islandCounter++;
    }

}
