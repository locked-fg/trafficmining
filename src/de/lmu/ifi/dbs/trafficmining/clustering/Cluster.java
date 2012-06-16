package de.lmu.ifi.dbs.trafficmining.clustering;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author skurtz
 */
public class Cluster implements ExtendedClusterObject {

    private List<ExtendedClusterObject> items = new ArrayList<>();
    private int id;

    public Cluster(int id) {
        this.id = id;
    }

    public Cluster(int id, Cluster node) {
        this.id = id;
        items.add(node);
    }

    public Cluster(int id, Route node) {
        this.id = id;
        items.add(node);
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void add(ExtendedClusterObject object) {
        if (object instanceof Route) {
            add((Route) object);
        }
        if (object instanceof Cluster) {
            add((Cluster) object);
        }
    }

    public void add(Cluster node) {
        items.add(node);
    }

    public void add(Route node) {
        items.add(node);
    }

    public ClusterObject getChild(int i) {
        if (items.isEmpty() || items.size() < i) {
            return null;
        } else {
            return items.get(i);
        }
    }

    public int size() {
        return items.size();
    }

    public List<Route> getRoutes() {
        ArrayList<Route> list = new ArrayList<>();
        for (ExtendedClusterObject o : items) {
            if (o instanceof Route) {
                list.add((Route) o);
            }
            if (o instanceof Cluster) {
                List<Route> tmp = ((Cluster) o).getRoutes();
                for (Route p : tmp) {
                    list.add(p);
                }
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return "Cluster " + id;
    }

    public boolean contains(Route route) {
        if (!(items.isEmpty())) {
            List<Route> list = getRoutes();
            return list.contains(route);
        } else {
            return false;
        }
    }

    public boolean contains(int id) {
        if (!items.isEmpty()) {
            for (Route r : getRoutes()) {
                if (r.getId() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> getUnits() {
        for (ExtendedClusterObject co : items) {
            if (co instanceof Route) {
                return co.getUnits();
            }
        }
        return items.get(0).getUnits();
    }

    @Override
    public List<Double> getMinCosts() {
        List<Double> minCosts = new ArrayList<>();
        List<Double> tmp = null;
        for (int i = 0; i < getUnits().size(); i++) {
            minCosts.add(Double.MAX_VALUE);
        }
        for (ExtendedClusterObject co : items) {
            tmp = co.getMinCosts();
            for (int i = 0; i < tmp.size(); i++) {
                if (tmp.get(i) < minCosts.get(i)) {
                    minCosts.set(i, tmp.get(i));
                }
            }
        }
        return minCosts;
    }

    @Override
    public List<Double> getMaxCosts() {
        List<Double> maxCosts = new ArrayList<>();
        List<Double> tmp;
        for (int i = 0; i < getUnits().size(); i++) {
            maxCosts.add(Double.MIN_VALUE);
        }
        for (ExtendedClusterObject co : items) {
            tmp = co.getMaxCosts();
            for (int i = 0; i < tmp.size(); i++) {
                if (tmp.get(i) > maxCosts.get(i)) {
                    maxCosts.set(i, tmp.get(i));
                }
            }
        }
        return maxCosts;
    }
}
