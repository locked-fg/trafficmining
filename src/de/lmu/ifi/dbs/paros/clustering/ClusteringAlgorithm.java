/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.lmu.ifi.dbs.paros.clustering;

import de.lmu.ifi.dbs.paros.result.Result;



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
