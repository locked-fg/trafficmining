package de.lmu.ifi.dbs.trafficmining.noOsm;

import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;

public class WeightedLink extends OSMLink<WeightedNode2D<WeightedLink>> {

    private final float[] weights;

    public WeightedLink(WeightedNode2D src, WeightedNode2D dst, float[] w) {
        super(src, dst);
        weights = w;
    }
    
    public WeightedLink(WeightedNode2D src, WeightedNode2D dst, float[] w, boolean isOneWay) {
        super(src, dst,isOneWay);
        weights = w;
    }

    public float[] getWeights() {
        return weights;
    }
}
