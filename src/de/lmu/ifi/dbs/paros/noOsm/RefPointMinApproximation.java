package de.lmu.ifi.dbs.paros.noOsm;

import de.lmu.ifi.dbs.paros.noOsm.WeightedNode2D;

@Deprecated
public class RefPointMinApproximation implements Approximation {

    float[] weights;

    public RefPointMinApproximation(float[] w) {
        weights = w;
    }

    @Override
    public float estimate(WeightedNode2D node1, WeightedNode2D node2) {
        float maxVal = 0;
        for (int rf = 0; rf < node1.getRefDist()[0].length; rf++) {
            float val = 0;
            for (int p = 0; p < node1.getRefDist().length; p++) {
                val += weights[p] * (Math.abs(node1.getRefDist()[p][rf] - node2.getRefDist()[p][rf]));
            }
            if (val > maxVal) {
                maxVal = val;
            }
        }
        return maxVal;
    }

    @Override
    public float estimateX(WeightedNode2D node1, WeightedNode2D node2, int x) {
        float maxVal = 0;
        for (int rf = 0; rf < node1.getRefDist()[0].length; rf++) {
            float val = 0;
            val = node1.getRefDist()[x][rf] - node2.getRefDist()[x][rf];
            if (val < 0) {
                val = -val;
            }
            if (val > maxVal) {
                maxVal = val;
            }
        }
        return maxVal;
    }
}
