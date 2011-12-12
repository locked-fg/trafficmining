package de.lmu.ifi.dbs.paros.noOsm;

import de.lmu.ifi.dbs.paros.graph.OSMNode;

public class WeightedNode2D<L extends WeightedLink> extends OSMNode<L> {

//    private float x;
//    private float y;
    private float[][] refDist;

    public WeightedNode2D(int n, float xVal, float yVal, float[][] refs) {
        super(n);
        setLat(yVal);
        setLon(xVal);
        refDist = refs;
    }

    public float[][] getRefDist() {
        return refDist;
    }

    public void setRefDist(float[][] refDist) {
        this.refDist = refDist;
    }

    /**
     * @deprecated use {@link #getLon()} instead
     * @return
     */
    public float getX() {
        return (float) getLon();
    }

    /**
     * @deprecated use {@link #getLat()} instead
     * @return
     */
    public float getY() {
        return (float) getLat();
    }

    @Override
    public String toString() {
        return "WeightedNode2D{" + "x=" + getLon() + ", y=" + getLat() + ", refDist=" + refDist + '}';
    }
}
//package de.lmu.ifi.dbs.paros.graph;
//
//@Deprecated
//public class WeightedNode2D<L extends WeightedLink> extends Node<L> {
//
//    private float x;
//    private float y;
//    private float[][] refDist;
//
//    public WeightedNode2D(int n, float xVal, float yVal, float[][] refs) {
//        super(n);
//        x = xVal;
//        y = yVal;
//        refDist = refs;
//    }
//
//    public float[][] getRefDist() {
//        return refDist;
//    }
//
//    public void setRefDist(float[][] refDist) {
//        this.refDist = refDist;
//    }
//
//    public float getX() {
//        return x;
//    }
//
//    public float getY() {
//        return y;
//    }
//
//    @Override
//    public String toString() {
//        return "WeightedNode2D{" + "x=" + x + ", y=" + y + ", refDist=" + refDist + '}';
//    }
//}
