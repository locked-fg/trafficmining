package de.lmu.ifi.dbs.trafficmining.clustering;

import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author skurtz
 */
public class IterativeDistWithHashing implements RouteDistanceWithPruning {

    private double splitWeight = 1;
    private double splitBasis = 0.5;
    private double mergeWeight = 0;
    private double mergeBasis = 0;
    private HashMap<Integer, Integer> table = new HashMap<Integer, Integer>();

    public IterativeDistWithHashing() {
        this.splitWeight = 1;
        this.splitBasis = 1;
        this.mergeWeight = 0;
        this.mergeBasis = 0;
    }

    public IterativeDistWithHashing(double splitWeight, double splitBasis, double mergeWeight, double mergeBasis) {
        this.splitWeight = splitWeight;
        this.splitBasis = splitBasis;
        this.mergeWeight = mergeWeight;
        this.mergeBasis = mergeBasis;
    }

    @Override
    public double getDist(Object object1, Object object2, double mindist) {
        if (object1 instanceof Path && object2 instanceof Path) {
            return getDist((Path) object1, (Path) object2, mindist);
        }
        if (object1 instanceof List && object2 instanceof Path) {
            return getDist((List<Path>) object1, (Path) object2, mindist);
        }
        if (object1 instanceof Path && object2 instanceof List) {
            return getDist((List<Path>) object2, (Path) object1, mindist);
        }
        if (object1 instanceof List && object2 instanceof List) {
            return getDist((List<Path>) object1, (List<Path>) object2, mindist);
        }
        return 0;
    }

    @Override
    public double getDist(List<Path> cluster, Path route, double pruningdist) {
        double dist = 0;

        for (Path route1 : cluster) {
            dist = getDist(route1, route, pruningdist);
            if (dist < pruningdist) {
                pruningdist = dist;
            }
        }
        return pruningdist;
    }

    @Override
    public double getDist(List<Path> cluster1, List<Path> cluster2, double pruningdist) {
        double dist = 0;

        for (Path route : cluster2) {
            dist = getDist(cluster1, route, pruningdist);
            if (dist < pruningdist) {
                pruningdist = dist;
            }
        }
        return pruningdist;
    }

    @Override
    public double getDist(Path referenceRoute, Path newRoute, double pruningdist) {
        List<Node> nodes0 = referenceRoute.getNodes();
        List<Node> nodes1 = newRoute.getNodes();
        int start[] = {0, 0};
        int stop[] = {0, 0};
        double dist = 0;
        int splitCount = 0;
        int mergeCount = 0;

        createNodeTable(referenceRoute, newRoute);

        while (stop[0] < nodes0.size() && stop[1] < nodes1.size()) {
            while (stop[0] < nodes0.size() && table.get(nodes0.get(stop[0]).getName()) > 1) {
                stop[0]++;
                stop[1]++;
            }
            mergeCount++;

            dist += this.mergeWeight * Math.pow(this.mergeBasis, mergeCount) * (stop[0] - start[0]);
            if (normalization(dist, nodes0.size(), nodes1.size()) >= pruningdist) {
                return Double.MAX_VALUE;
            }

            start[0] = stop[0];
            start[1] = stop[1];

            if (stop[0] < nodes0.size()) {
                stop = findNextEqualNodeWithHashing(nodes0, nodes1, start);
                splitCount++;

                dist += this.splitWeight * (double) Math.pow(this.splitBasis, mergeCount) * Math.abs((stop[0] - start[0]) + (stop[1] - start[1]));
                if (normalization(dist, nodes0.size(), nodes1.size()) >= pruningdist) {
                    return Double.MAX_VALUE;
                }

                start[0] = stop[0];
                start[1] = stop[1];
            }
        }
        return normalization(dist, nodes0.size(), nodes1.size());
    }

    @Override
    public double getDist(Path route, List<Path> cluster, double pruningdist) {
        return getDist(cluster, route, pruningdist);
    }

    @Override
    public double getDist(Object object1, Object object2) {
        return getDist(object1, object2, Double.MAX_VALUE);
    }

    @Override
    public double getDist(Path route1, Path route2) {
        return getDist(route1, route2, Double.MAX_VALUE);
    }

    @Override
    public double getDist(List<Path> cluster, Path route) {
        return getDist(cluster, route, Double.MAX_VALUE);
    }

    @Override
    public double getDist(Path route, List<Path> cluster) {
        return getDist(cluster, route, Double.MAX_VALUE);
    }

    @Override
    public double getDist(List<Path> cluster1, List<Path> cluster2) {
        return getDist(cluster1, cluster2, Double.MAX_VALUE);
    }

    private double normalization(double dist, int nodeCountRoute1, int nodeCountRoute2) {
        return dist * 2 / (nodeCountRoute1 + nodeCountRoute2);
    }

    private void createNodeTable(Path route0, Path route1) {
        table.clear();
        addHashMap(route0.getNodes());
        addHashMap(route1.getNodes());
    }

    private void addHashMap(List<Node> list) {
        for (Node n : list) {
            if (table.containsKey(n.getName())) {
                table.put(n.getName(), table.get(n.getName()) + 1);
            } else {
                table.put(n.getName(), 1);
            }
        }
    }

    private int[] findNextEqualNodeWithHashing(List<Node> nodes0, List<Node> nodes1, int[] start) {
        int stop[] = {start[0], start[1]};

        while (table.get(nodes0.get(stop[0]).getName()) == 1) {
            stop[0]++;
        }
        while (table.get(nodes1.get(stop[1]).getName()) == 1) {
            stop[1]++;
        }
        return stop;
    }
}
