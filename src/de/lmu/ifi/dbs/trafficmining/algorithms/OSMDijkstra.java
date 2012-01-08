package de.lmu.ifi.dbs.trafficmining.algorithms;

import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.result.Result;
import de.lmu.ifi.dbs.trafficmining.result.Simplex1Result;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import de.lmu.ifi.dbs.utilities.MutablePriorityObject;
import de.lmu.ifi.dbs.utilities.UpdatablePriorityQueue;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OSMDijkstra<N extends OSMNode<L>, L extends OSMLink<N>>
        extends Algorithm<N, OSMGraph<N, L>, Path> {

    private static final String STAT_RUNTIME = "Runtime";
    private static final String STAT_NUM_VISITED_NODES = "# of visited nodes";
    // -
    private static final Logger log = Logger.getLogger(OSMDijkstra.class.getName());
    public ATTRIBS myAttribs = ATTRIBS.FASTEST;
    // Results
//    private Simplex1Result s1 = new Simplex1Result();
//    private HashMap<N, WeightedPath> visited = new HashMap<N, WeightedPath>();
    private Simplex1Result s1;
    private HashMap<N, WeightedPath> visited;

    public enum ATTRIBS {

        SHORTEST, FASTEST
    }

    public ATTRIBS getMyAttribs() {
        return myAttribs;
    }

    public void setMyAttribs(ATTRIBS myAttribs) {
        this.myAttribs = myAttribs;
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
        for (OSMLink<N> aktLink : node1.getOutLinks()) {
            WeightedPath<N, L> aktPath = new WeightedPath(node1, aktLink, linkCost(aktLink));
            if (updateVisited(aktPath.getLast(), aktPath)) {
                q.insertIfBetter(new PriorityPath(aktPath.getCost(), aktPath));
            }
        }

        // now process queue and expand across the whole graph
        while (!q.isEmpty() && best > q.firstValue() && !Thread.interrupted()) {
            PriorityPath<WeightedPath<N, L>> p = q.removeFirst();

            // reached target with a (cheaper) path
            if (p.getPath().getLast() == dest && p.getPriority() < best) {
                best = p.getPriority();
                bestPath = p.getPath();
            }

            for (OSMLink<N> link : p.getPath().getLast().getOutLinks()) {
                // don't follow the incoming link back again
                if (p.getPath().contains(link)) {
                    continue;
                }
                WeightedPath<N, L> newPath = new WeightedPath(p.getPath(), link, linkCost(link));
                if (updateVisited(newPath.getLast(), newPath)) {
                    q.insertIfBetter(new PriorityPath(newPath.getCost(), newPath));
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

    private double linkCost(OSMLink link) {
        if (myAttribs == ATTRIBS.FASTEST) {
            double v = link.getSpeed(); //km/h
//            @FIXME, CHECKME
            double x = link.getLength() / 1000; //meter/1000 -> km
            double time = x / v; //km / (km/h) -> h
            if (time <= 0) {
                log.log(Level.WARNING, "Invalid time at link: {0} -> length: {1}km, speed: {2}km/h => time: {3}h", new Object[]{link, x, v, time});
                time = 1;
            }

            return time;
        } else {
            return link.getLength() / 1000; //meter/1000 -> km
        }
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
            getStatistics().putPath(path, OSMUtils.getPathInfos(path.getNodes()));
        }

        // fill result
        if (myAttribs.equals(ATTRIBS.SHORTEST)) {
            s1.setUnits("km");
            s1.setAttributes("DISTANCE");
        } else {
            s1.setUnits("h");
            s1.setAttributes("TIME");
        }
        s1.addResult(path, path.getCost());

        long b = System.currentTimeMillis();
        getStatistics().put(STAT_RUNTIME, String.format("%,d ms", b - a));
        buildStatistics(path);
    }

    @Override
    public String getName() {
        return "OSMDijkstra";
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
}

class WeightedPath<N extends OSMNode<L>, L extends OSMLink<N>>
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