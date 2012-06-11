package de.lmu.ifi.dbs.trafficmining.algorithms;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.result.Result;
import de.lmu.ifi.dbs.trafficmining.result.Simplex1Result;
import de.lmu.ifi.dbs.trafficmining.utils.GeoDistance;
import de.lmu.ifi.dbs.trafficmining.utils.GreatcircleDistance;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import de.lmu.ifi.dbs.utilities.MutablePriorityObject;
import de.lmu.ifi.dbs.utilities.UpdatablePriorityQueue;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * simple A* search algorithm
 *
 * @author Franz
 * @param <N>
 * @param <L>
 */
public class AStar<N extends Node<L>, L extends Link<N>>
        extends Algorithm<N, Graph<N, L>, Path> {

    private static final String STAT_RUNTIME = "Runtime";
    private static final String STAT_NUM_VISITED_NODES = "# of visited nodes";
    // -
    private static final Logger log = Logger.getLogger(AStar.class.getName());
    private double weight = 1;
    // Results
    private Simplex1Result s1;
    private HashMap<N, WeightedPath> visited;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Conditionally update the table of visited nodes.
     *
     * @param node
     * @param pathToNode
     * @return
     */
    private boolean updateVisited(N node, WeightedPath pathToNode) {
        WeightedPath oldCost = visited.get(node);
        if (oldCost == null) { // node was not yet visited
            visited.put(node, pathToNode);
            return true;
        } else if (oldCost.getCost() >= pathToNode.getCost()) { // found a better way to the node
            visited.put(node, pathToNode);
            return true;
        }
        // the node is already reached by a better path
        return false;
    }

    private WeightedPath simpleShortestPath(N node1, N dest) {
        if (node1 == dest) {
            return null;
        }

        double best = Double.MAX_VALUE;
        WeightedPath<N, L> bestPath = null;
        UpdatablePriorityQueue<PriorityPath> q = new UpdatablePriorityQueue<>(true);

        // initialize queue
        for (Link<N> aktLink : node1.getOutLinks()) {
            WeightedPath<N, L> aktPath = new WeightedPath(node1, aktLink, linkCost(aktLink));
            if (updateVisited(aktPath.getLast(), aktPath)) {
                q.insertIfBetter(new PriorityPath(aktPath.getCost(), aktPath));
            }
        }

        // now process queue and expand across the whole graph
        GeoDistance dist = new GreatcircleDistance();
        while (!q.isEmpty() && best > q.firstValue() && !Thread.interrupted()) {
            PriorityPath<WeightedPath<N, L>> p = q.removeFirst();

            // reached target with a (cheaper) path
            if (p.getPath().getLast() == dest && p.getPriority() < best) {
                best = p.getPriority();
                bestPath = p.getPath();
            }

            for (Link<N> link : p.getPath().getLast().getOutLinks()) {
                // don't follow the incoming link back again
                if (p.getPath().contains(link)) {
                    continue;
                }
                WeightedPath<N, L> newPath = new WeightedPath(p.getPath(), link, linkCost(link));
                if (updateVisited(newPath.getLast(), newPath)) {
                    double newCost = newPath.getCost();
                    double heuristic = weight * dist.distance(newPath.getLast(), dest) / 100d;
                    q.insertIfBetter(new PriorityPath(newCost + heuristic, newPath));
                }
            }
        }

        int visitedSize = visited.size();
        int totalSize = getGraph().getNodes().size();
        getStatistics().put(STAT_NUM_VISITED_NODES, String.format("%d / %d = %d%%",
                visitedSize, totalSize, visitedSize * 100 / totalSize));
        getStatistics().setVisitedNodes(visited.keySet());
        return bestPath;
    }

    private double linkCost(Link link) {
        return link.getLength() / 1000d; //meter/1000 -> km
    }

    @Override
    public Result getResult() {
        return s1;
    }

    @Override
    public void run() {
        long a = System.currentTimeMillis();

        // init result
        s1 = new Simplex1Result();
        visited = new HashMap<>();

        List<N> nodes = getNodes();
        WeightedPath path = simpleShortestPath(nodes.get(0), nodes.get(nodes.size() - 1));
        if (path != null) {
            getStatistics().putPath(path, OSMUtils.getPathInfos(path.getParentNodes()));
        }

        // fill result
        s1.setUnits("km");
        s1.setAttributes("DISTANCE");
        s1.addResult(path, path.getCost());

        long b = System.currentTimeMillis();
        getStatistics().put(STAT_RUNTIME, String.format("%,d ms", b - a));
        buildStatistics(path);
    }

    @Override
    public String getName() {
        return "A*";
    }

    class PriorityPath<P extends Path> implements MutablePriorityObject<P> {

        private double prio;
        private final P path;

        public PriorityPath(double p, P pa) {
            prio = p;
            path = pa;
        }

        @Override
        public Comparable getKey() {
            return path.getLast().getName();
        }

        @Override
        public double getPriority() {
            return prio;
        }

        @Override
        public void setPriority(double newPriority) {
            prio = newPriority;
        }

        public P getPath() {
            return path;
        }

        @Override
        public P getValue() {
            return path;
        }

        @Override
        public String toString() {
            return prio + " / " + path.toString();
        }
    }

    class WeightedPath<N extends Node<L>, L extends Link<N>>
            extends Path<WeightedPath, N, L> {

        private final double cost;

        /**
         * @param n start node
         * @param l new link
         * @param cost costs
         */
        WeightedPath(N n, L l, double cost) {
            super(n, l);
            this.cost = cost;
            assert getParent() == null || !getParent().contains(getLast()) : "Loop in: " + toString();
            assert cost >= 0 : "negative cost?";
        }

        WeightedPath(WeightedPath p, L l, double newCost) {
            super(p, l);
            this.cost = p.getCost() + newCost;
            assert cost >= 0 : "negative cost?";
        }

        public double getCost() {
            return cost;
        }
    }
}
