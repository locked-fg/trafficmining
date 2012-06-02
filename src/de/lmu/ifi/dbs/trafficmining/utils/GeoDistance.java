package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;

/**
 * Interface for various distance measures between nodes or links.
 *
 * As distances on the earth are not trivial 2d like distance measures, there
 * exist several distance measures with the trade off between accuracy and
 * computation speed.
 *
 * Implementing classes should always give a hint about both accuracy and
 * computation time. For example it might be rather irrelevant to regard the
 * difference between a spherical and an ellipitcal model of the earth if the
 * distance is just about some meters, whereas a possibly set height attribute
 * might be more relevant.
 *
 * Also, all distance measures should return the result in meters.
 *
 * @author graf
 */
public interface GeoDistance {

    /**
     * Compute the length of a link using this distance measure.
     *
     * @param link
     * @return length of the link in m
     */
    public double length(Link<? extends Node> link);

    /**
     * Computes the distance between to OSMNodes
     *
     * @param a
     * @param b
     * @return distance in meters - possibly regarding height values
     */
    public double distance(Node a, Node b);

    /**
     * Computes the distance between to coordinate values
     *
     * @param a
     * @param b
     * @return distance in meters
     */
    public double distance(double latitudeA, double longitudeA, double latitudeB, double longitudeB);
}
