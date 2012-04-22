package de.lmu.ifi.dbs.trafficmining.utils;

import java.util.HashSet;
import java.util.Set;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

/**
 *
 * @author Franz
 */
public class MapBounds {

    public double top, right, bottom, left;

    public MapBounds(GeoPosition topLeft, GeoPosition bottomRight) {
        left = topLeft.getLongitude();
        top = topLeft.getLatitude();
        right = bottomRight.getLongitude();
        bottom = bottomRight.getLatitude();

        if (left > right) {
            double tmp = left;
            left = right;
            right = tmp;
        }
        if (bottom > top) {
            double tmp = bottom;
            bottom = top;
            top = tmp;
        }
    }

    public MapBounds(double top, double right, double bottom, double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public MapBounds(Bound bound) {
        this.top = bound.getTop();
        this.right = bound.getRight();
        this.bottom = bound.getBottom();
        this.left = bound.getLeft();
    }

    public Set<GeoPosition> toSet() {
        Set<GeoPosition> set = new HashSet<>();
        set.add(new GeoPosition(top, left));
        set.add(new GeoPosition(top, right));
        set.add(new GeoPosition(bottom, left));
        set.add(new GeoPosition(bottom, right));
        return set;
    }

    @Override
    public String toString() {
        return "MapBounds{" + "top=" + top + ", right=" + right + ", bottom=" + bottom + ", left=" + left + '}';
    }
}