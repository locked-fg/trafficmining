package de.lmu.ifi.dbs.trafficmining.algorithms.skyline;

import de.lmu.ifi.dbs.trafficmining.graph.Node;
import java.util.List;
import java.util.Map;

// TODO use embedding class instead of embedding map and refpoints list
public class OSMRefPointMinApproximation<N extends Node> implements
        OSMApproximation<N> {

    private final float[] weights;
    private final Map<N, NodeWrapper<N>> embedding;
    private final List<N> refPoints;

    public OSMRefPointMinApproximation(float[] weights, Map<N, NodeWrapper<N>> embedding, List<N> refPoints) {
        this.weights = weights;
        this.embedding = embedding;
        this.refPoints = refPoints;
    }

    @Override
    public float estimate(N node1, N node2) {
        float maxVal = Float.MIN_VALUE;
        float val;
        for (N refPoint : refPoints) {
            float[] refDists1 = embedding.get(node1).getRefDist(refPoint);
            float[] refDists2 = embedding.get(node2).getRefDist(refPoint);
            // not all refPoints can be reached from each other node (islands for example!)
            if (refDists1 == null || refDists2 == null) {
                continue;
            }

            val = 0;
            for (int i = 0; i < refDists2.length; i++) {
                val += weights[i] * Math.abs(refDists1[i] - refDists2[i]);
            }
            maxVal = val > maxVal ? val : maxVal;
        }
        assert !Float.isNaN(maxVal) : "NaN";
        return maxVal;
    }

    @Override
    public float estimateX(N node1, N node2, int p) {
        float maxVal = Float.MIN_VALUE;
        float refDist1, refDist2, val;
        for (N refPoint : refPoints) {
            refDist1 = embedding.get(node1).getRefDist(p, refPoint);
            refDist2 = embedding.get(node2).getRefDist(p, refPoint);
            val = weights[p] * Math.abs(refDist1 - refDist2);
            maxVal = (val > maxVal) ? val : maxVal;
        }
        return maxVal;
    }
}
