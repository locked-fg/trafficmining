
package de.lmu.ifi.dbs.trafficmining.clustering;

import java.util.List;

/**
 *
 * @author skurtz
 */
public interface ExtendedClusterObject extends ClusterObject{

    public List<String> getUnits();

    public List<Double> getMinCosts();

    public List<Double> getMaxCosts();

}