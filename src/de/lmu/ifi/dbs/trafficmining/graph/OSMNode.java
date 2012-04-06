package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class OSMNode<L extends OSMLink> extends Node<L> {

    private double lat;
    private double lon;
    private double height = Double.NaN;
    private String name = null;
    private HashMap<String, String> attr = null;

    public OSMNode(int id) {
        super(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    /**
     * Returns the elevation data of this node. If no explicit elevation data
     * was set, Double.NAN is returned.
     *
     * @return elevation in meters or Double.NaN if no elevation was set.
     */
    public double getHeight() {
        return height;
    }

    public GeoPosition getGeoPosition() {
        return new GeoPosition(lat, lon);
    }

    /**
     * Set's a certain attribute key/value pair. For both key and value, the
     * intern representation is used to (hopefully) save memory.
     *
     * @param key
     * @param value
     * @see String#intern()
     */
    public void setAttr(String key, String value) {
        if (attr == null) {
            attr = new HashMap<>(1);
        }
        this.attr.put(key.intern(), value.intern());
    }

    /**
     * returns the map of attributes. The map is immutable if it is the empty
     * map.
     *
     * @return
     */
    public Map<String, String> getAttr() {
        if (attr == null) {
            return Collections.EMPTY_MAP;
        }
        return attr;
    }

    public String getAttr(String key) {
        return attr != null ? attr.get(key) : null;
    }

    @Override
    public String toString() {
        if (name == null) {
            return super.toString();
        } else {
            return super.toString() + ", " + name;
        }
    }
}
