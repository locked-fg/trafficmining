
package de.lmu.ifi.dbs.trafficmining.clustering;

import de.lmu.ifi.dbs.trafficmining.result.Result;



/**
 *
 * @author skurtz
 */
public interface ClusteringAlgorithm {

    public void setDistance(RouteDistance rd);

    public void setInput(Result input);

    public void start();

    public Cluster getResult();

}
