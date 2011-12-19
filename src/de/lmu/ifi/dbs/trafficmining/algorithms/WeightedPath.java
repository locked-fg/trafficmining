package de.lmu.ifi.dbs.trafficmining.algorithms;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;

public class WeightedPath<N extends OSMNode<L>, L extends OSMLink<N>>
        extends Path<WeightedPath, N, L> {

    private final double cost;

    /**
     * @param n start node
     * @param l new link
     * @param cost costs
     */
    public WeightedPath(N n, L l, double cost) {
        super(n, l);
        this.cost = cost;
        assert getParent() == null || !getParent().contains(getLast()) : "Loop in: " + toString();
        assert cost >= 0 : "negative cost?";
    }

    public WeightedPath(WeightedPath p, L l, double newCost) {
        super(p, l);
        this.cost = p.getCost() + newCost;
        assert cost >= 0 : "negative cost?";
    }

    public double getCost() {
        return cost;
    }
}
