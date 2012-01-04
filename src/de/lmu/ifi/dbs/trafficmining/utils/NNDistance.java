package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import java.util.Iterator;
import java.util.List;

/**
 * Distance that should only be used to detect &gt;, &lt; relations between
 * nodes as it simply computes the difference of the latitude, longitude values.
 *
 * Very fast (no square root in distance approximatin), very inaccurate.
 * 
 * @author graf
 */
public class NNDistance implements GeoDistance {

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

    @Override
    public double distance(OSMNode a, OSMNode b) {
        return distance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
    }

    @Override
    public double distance(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double y = latitudeA - latitudeB;
        double x = longitudeA - longitudeB;
        return x * x + y * y;

    }
}
