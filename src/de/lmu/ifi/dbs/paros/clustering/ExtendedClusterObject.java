/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.lmu.ifi.dbs.paros.clustering;

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