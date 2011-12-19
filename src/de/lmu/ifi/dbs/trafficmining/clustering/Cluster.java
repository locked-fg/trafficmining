package de.lmu.ifi.dbs.trafficmining.clustering;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author skurtz
 */
public class Cluster implements ExtendedClusterObject {

    private ArrayList<ExtendedClusterObject> items = new ArrayList<ExtendedClusterObject>();
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
        ArrayList<Route> list = new ArrayList<Route>();
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

//    public List<Path> getPaths() {
//        List<Route> list = getRoutes();
//        List<Path> ret = new ArrayList<Path>();
//        for (Route r : list) {
//            ret.add(r.getComplexPath());
//        }
//        return ret;
//    }

    @Override
    public String toString() {
        return "Cluster " + id;
    }

//    public String printTree() {
//        return printTree(0);
//    }

    private String printTree(int level) {
        String prefix = "";
        String separator = "|--";
        String tail = "";

        for (int i = 0; i < level; i++) {
            prefix = prefix + separator;
        }

        for (ExtendedClusterObject o : items) {
            if (o instanceof Route) {
                tail = tail + prefix + "|--" + ((Route) o).toString() + "\n";
            }
            if (o instanceof Cluster) {
                tail = tail + ((Cluster) o).printTree(level + 1);
            }
        }

        return prefix + toString() + "\n" + tail;
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
        if (!(items.isEmpty())) {
            List<Route> list = getRoutes();
            for (Route r : list) {
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
        List<Double> minCosts = new ArrayList<Double>();
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
        List<Double> maxCosts = new ArrayList<Double>();
        List<Double> tmp = null;
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
