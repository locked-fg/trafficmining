package de.lmu.ifi.dbs.trafficmining.algorithms.skyline;

import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.util.ArrayList;
import java.util.List;

public class OSMComplexPath<N extends Node<L>, L extends Link<N>>  
     extends Path<OSMComplexPath, N, L> {

    private final float[] cost;
    private boolean processed = false;

    /**
     * @param n start node
     * @param n end node
     * @param cost costs
     */
    public OSMComplexPath(N start, N end, float[] cost) {
        super(start, end);
        this.cost = cost;
        assert cost[0] >= 0 : "negative cost?";
    }

    /**
     * @param n start node
     * @param l new link
     * @param cost costs
     */
    public OSMComplexPath(N n, L l, float[] cost) {
        super(n, l);
        this.cost = cost;
        assert getParent() == null || !getParent().contains(getLast()) : "Loop in: " + toString();
        assert cost[0] >= 0 : "negative cost?";
    }

    public OSMComplexPath(OSMComplexPath p, L l, float[] additionalCost) {
        super(p, l);
        this.cost = p.getCost().clone();
        Arrays2.add(this.cost, additionalCost);
        assert cost[0] >= 0 : "negative cost?";
    }

    OSMComplexPath(OSMComplexPath p, N n, float[] additionalCost) {
        super(p, n);
        this.cost = p.getCost().clone();
        Arrays2.add(this.cost, additionalCost);
        assert cost[0] >= 0 : "negative cost?";
    }

    public float prefVal(float[] w) {
        if (w.length != cost.length) {
            throw new IndexOutOfBoundsException("lengths do not match: " + w.length + " <> " + cost.length);
        }
        float result = 0;
        for (int d = 0; d < w.length; d++) {
            result += w[d] * cost[d];
        }
        return result;
    }

    /**
     * Reverses the path
     * @param n
     * @return
     */
//    @Override
    public OSMComplexPath reverse() {
        List<OSMComplexPath> subPaths = new ArrayList<>(this.getLength());
        OSMComplexPath<N, L> p = (OSMComplexPath<N, L>) getParent();
        while (p != null) {
            subPaths.add(p);
            p = (OSMComplexPath<N, L>) p.getParent();
        }
        OSMComplexPath<N, L> result = null;
        float[] loc_cost = this.cost.clone();
        for (OSMComplexPath<N, L> subPath : subPaths) {
            float[] aktCosts = Arrays2.sub(loc_cost, subPath.cost, null);
            if (result == null) {
                result = new OSMComplexPath<>(getLast(), subPath.getLast(), aktCosts);
            } else {
                result = new OSMComplexPath<>(result, subPath.getLast(), aktCosts);
            }
            loc_cost = subPath.cost;
        }
        if (result != null) {
            result = new OSMComplexPath(result, this.getFirst(), loc_cost);
        } else {
            result = new OSMComplexPath(this.getLast(), this.getFirst(), loc_cost);
        }
        return result;
    }

    /**
     * Appends one path to another
     * @param n
     * @return
     */
    public OSMComplexPath append(OSMComplexPath<N, L> other) {
        if (!contains(other.getLast()) && !contains(other.getFirst())) {
            throw new IllegalArgumentException("Paths not connected! " + toString() + " <> " + other.toString());
        }
        if (contains(other.getLast()) && contains(other.getFirst())) {
            throw new IllegalArgumentException("Paths overlapping!");
        }
        if (getLast().equals(other.getFirst())) {
            other = other.reverse();
        }
        float[] appendCost = other.cost;
        other = other.getParent(); // otherwise we have a loop: A-B + B-C -> A-B-B-C
        OSMComplexPath<N, L> newPath = this;
        while (other != null) {
            float[] aktCost = new float[cost.length];
            for (int d = 0; d < aktCost.length; d++) {
                aktCost[d] += appendCost[d] - other.cost[d];
            }
            newPath = new OSMComplexPath<>(newPath, other.getLast(), aktCost);
            other = other.getParent();
        }
        return newPath;
    }

    /**
     * Tests, is one path dominates another. A path p1 is dominated by p2 another if
     * all cost factors of p2 are &lt;= p1.
     *
     * If not all cost factors are dominated, none of the paths is dominated.
     *
     * Returns 1 if p1 is ALWAYS better than p2,
     * returns -1 if  p2 is ALWAYS better p1,
     * returns 0 if p1[i]==p2[i] for all cost factors
     *
     * @param p1
     * @param p2
     * @param dest
     * @param minDist
     * @return -1,0,1
     */
    public int dominates(OSMComplexPath p2, N dest, OSMApproximation minDist, float[] weights) {
        if (getLast() != dest) {
            return 0;
        }
        if (p2.getLast() == dest) {
            assert getLast() == dest && p2.getLast() == dest : "p1,p2 should be at dest but are not.";
            boolean p1betterp2 = false;
            boolean p2betterp1 = false;
            for (int d = 0; d < p2.getCost().length; d++) {
                p2betterp1 = getCost()[d] > p2.getCost()[d] ? true : p2betterp1;
                p1betterp2 = p2.getCost()[d] > getCost()[d] ? true : p1betterp2;
            }
            if (p1betterp2 && !p2betterp1) { // p1 dominates p2
                return 1;
            } else if (p2betterp1 && !p1betterp2) { // p2 dominates p1
                return -1;
            } else {
                return 0;
            }
        } else {
            boolean p1betterp2 = false;
            for (int d = 0; d < p2.getCost().length; d++) {
                if (weights[d] == 0) {
                    continue;
                }
                float p2min = p2.getCost()[d] + minDist.estimateX(p2.getLast(), dest, d);
                if (getCost()[d] > p2min) {
                    return 0;
                }
                if (p2min > getCost()[d]) {
                    p1betterp2 = true;
                }
            }
            if (p1betterp2) {
                return 1;
            }
            return 0;
        }
    }

    public float[] getCost() {
        return cost;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed() {
        processed = true;
    }
}
