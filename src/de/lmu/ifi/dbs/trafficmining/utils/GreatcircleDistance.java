package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import java.util.Iterator;
import java.util.List;

/**
 * Class used to compute the gread circle distance between nodes / coordinates.
 *
 * By using a spherical model of the earth, this class is fast but not too
 * accurate for very long distances.
 *
 * @author graf
 */
public class GreatcircleDistance implements GeoDistance {

    /**
     * Average earth radius
     *
     * @see http://de.wikipedia.org/wiki/Erdradius
     */
    private final static double radius = 6371000.785; // ~earth radius in m
    private final static double PI180 = Math.PI / 180;

    /**
     * Compute the distance between to nodes.
     *
     * The height data is only taken into account if BOTH nodes have height
     * values (which means getHeight() returns s.th that is NOT NaN for both
     * nodes). Otherwise, only the 2d distance is computed.
     *
     * @param a
     * @param b
     * @return distance (possibly) regarding elevation data in m
     */
    @Override
    public double distance(OSMNode a, OSMNode b) {
        // 2d distance
        double d = distance(a.getLat(), a.getLon(), b.getLat(), b.getLon());

        // now integrate elevation
        double ha = a.getHeight();
        double hb = b.getHeight();
        if (!Double.isNaN(ha) && !Double.isNaN(hb)) {
            double heightDiff = a.getHeight() - b.getHeight();
            d = Math.sqrt(d * d + heightDiff * heightDiff);
        }
        return d;
    }

    /**
     * Computes the distance betwen two coordinates.
     *
     * @param lat1 Latitude of node A
     * @param lon1 Longitude of node A
     * @param lat2 Latitude of node B
     * @param lon2 Longitude of node B
     * @return the distance between both coordinates in m
     */
    @Override
    public double distance(double lat1, double lon1, double lat2, double lon2) {
        double lat = (lat1 - lat2) * PI180;
        double lon = (lon1 - lon2) * PI180;

        double latm = (lat1 + lat2) / 2;
        double lonSquare = lon * Math.cos(latm);
        lonSquare *= lonSquare;

        double d = radius * Math.sqrt(lat * lat + lonSquare);
        return d;
    }

    /**
     * Cmoputes the length of a link by summing up the individual distances of
     * nodes obtained by {@link OSMLink#getNodes()}. Elevation data is used if
     * it is set explicitly in the according nodes.
     *
     * @param link
     * @return the length of the link in meters
     * @see OSMLink#getNodes()
     */
    @Override
    public double length(OSMLink<? extends OSMNode> link) {
        List<? extends OSMNode> nodes = link.getNodes();
        double length = 0;

        // don't use a for loop here as link need not be a random access list.
        Iterator<? extends OSMNode> it = nodes.iterator();
        OSMNode b, a = it.next();
        while (it.hasNext()) {
            b = it.next();
            length += distance(a, b);
            a = b;
        }

        return length;
    }
}
