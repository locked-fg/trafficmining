package de.lmu.ifi.dbs.trafficmining.algorithms.skyline;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Wrapper class that contains the distances from a specific node (source) to
 * all reference points.
 */
public class NodeWrapper<N> {

    private final N source;
    private final int numAttributes;
    private final HashMap<N, float[]> distances; // target -> distances

    public NodeWrapper(N source, int numAttributes) {
        this.distances = new HashMap<N, float[]>();
        this.source = source;
        this.numAttributes = numAttributes;
    }

    public int mapSize() {
        return distances.size();
    }

    public void setRefDist(int attributeIndex, N refNode, float value) {
        float[] dists = distances.get(refNode);
        if (dists == null) {
            dists = new float[numAttributes];
            Arrays.fill(dists, Float.NaN);
            distances.put(refNode, dists);
        }
        dists[attributeIndex] = value;
    }

    public void insert(N refNode, float[] weights) {
        distances.put(refNode, weights);
    }

    public float getRefDist(int attributeIndex, N refNode) {
        float[] dists = distances.get(refNode);
        if (dists != null) {
            return dists[attributeIndex];
        }
        return Float.NaN;
    }

    public float[] getRefDist(N refNode) {
        return distances.get(refNode);
    }

    public N getSource() {
        return source;
    }

    public HashMap<N, float[]> getDistances() {
        return distances;
    }

    @Override
    public String toString() {
        return "Wrap(" + source + "->" + distances.size() + " reference points)";
    }
}
