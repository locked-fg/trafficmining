package de.lmu.ifi.dbs.paros.algorithms.skyline;

import de.lmu.ifi.dbs.paros.graph.Node;

public interface OSMApproximation<N extends Node> {

    public float estimate(N node1, N node2);

    public float estimateX(N node1, N node2, int attributeIndex);
}
