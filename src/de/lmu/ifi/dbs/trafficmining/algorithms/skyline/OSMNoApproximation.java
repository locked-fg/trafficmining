package de.lmu.ifi.dbs.trafficmining.algorithms.skyline;

import de.lmu.ifi.dbs.trafficmining.graph.Node;

public class OSMNoApproximation<N extends Node> implements OSMApproximation<N> {

    @Override
    public float estimate(N node1, N node2) {
        return 0;
    }

    @Override
    public float estimateX(N node1, N node2, int attributeIndex) {
        return 0;
    }
}
