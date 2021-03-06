package de.lmu.ifi.dbs.trafficmining.clustering;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author skurtz
 */
public class Route implements ExtendedClusterObject {

    private int id = 0;
    private Path p = null;
    private List<String> units = null;
    private List<Double> costs = null;

    public Route(int id, Path p) {
        this.id = id;
        this.p = p;
    }

    public Route(int id, Path p, List<String> units, List<Double> costs) {
        this.id = id;
        this.p = p;
        this.units = units;
        this.costs = costs;
    }

    public Route(int id, Path p, List<String> units, Double costs[]) {
        this.id = id;
        this.p = p;
        this.units = units;
        this.costs = new ArrayList<>();
        this.costs.addAll(Arrays.asList(costs));
    }

    public Path getComplexPath() {
        return p;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Route " + id;
    }

    @Override
    public List<String> getUnits() {
        return units;
    }

    @Override
    public List<Double> getMinCosts() {
        return costs;
    }

    @Override
    public List<Double> getMaxCosts() {
        return costs;
    }
}
