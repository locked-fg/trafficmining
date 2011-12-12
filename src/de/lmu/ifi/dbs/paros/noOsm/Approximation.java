package de.lmu.ifi.dbs.paros.noOsm;

import de.lmu.ifi.dbs.paros.noOsm.WeightedNode2D;

@Deprecated
public interface Approximation {

    public float estimate(WeightedNode2D node1, WeightedNode2D node2);

    public float estimateX(WeightedNode2D node1, WeightedNode2D node2, int x);
}
