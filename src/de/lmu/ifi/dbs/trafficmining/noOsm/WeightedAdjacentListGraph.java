package de.lmu.ifi.dbs.trafficmining.noOsm;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import java.util.logging.Logger;

@Deprecated
public class WeightedAdjacentListGraph<N extends WeightedNode2D<L>, L extends WeightedLink>
        extends Graph<N, L> {

    private static final Logger log = Logger.getLogger(WeightedAdjacentListGraph.class.getName());
    private int numAttributes;

    public WeightedAdjacentListGraph() {
    }

    public int getNumAttributes() {
        return numAttributes;
    }

    public void setNumAttributes(int numAttributes) {
        this.numAttributes = numAttributes;
    }

    public N addNode(int name, float x, float y) {
        N node = (N) new WeightedNode2D<WeightedLink>(name, x, y, null);
        super.addNode(node);
        return node;
    }
}
