package de.lmu.ifi.dbs.trafficmining.clustering;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.util.List;

/**
 *
 * @author skurtz
 */
public interface RouteDistance {

    public double getDist(Object object1, Object object2);

    public double getDist(Path route1, Path route2);

    public double getDist(List<Path> cluster, Path route);

    public double getDist(Path route, List<Path> cluster);

    public double getDist(List<Path> cluster1, List<Path> cluster2);
}
