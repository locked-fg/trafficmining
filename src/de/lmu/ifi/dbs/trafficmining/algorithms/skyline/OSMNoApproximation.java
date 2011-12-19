package de.lmu.ifi.dbs.trafficmining.algorithms.skyline;

import de.lmu.ifi.dbs.trafficmining.graph.Node;

public class OSMNoApproximation<N extends Node> implements OSMApproximation<N> {

    @Override
    public float estimate(Node node1, Node node2) {
        return 0;
    }

    @Override
    public float estimateX(Node node1, Node node2, int attributeIndex) {
        return 0;
    }
}
