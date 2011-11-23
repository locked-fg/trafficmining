package de.lmu.ifi.dbs.paros.clustering;

import de.lmu.ifi.dbs.paros.graph.Path;
import java.util.List;

/**
 *
 * @author skurtz
 */
public interface RouteDistanceWithPruning extends RouteDistance {

    public double getDist(Object object1, Object object2, double mindist);

    public double getDist(Path route1, Path route2, double pruningdist);

    public double getDist(List<Path> cluster, Path route, double pruningdist);

    public double getDist(Path route, List<Path> cluster, double pruningdist);

    public double getDist(List<Path> cluster1, List<Path> cluster2, double pruningdist);

}
