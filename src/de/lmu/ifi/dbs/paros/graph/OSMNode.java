package de.lmu.ifi.dbs.paros.graph;

import de.lmu.ifi.dbs.paros.graph.Node;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdesktop.swingx.mapviewer.GeoPosition;

public class OSMNode<L extends OSMLink> extends Node<L> {

    private double lat;
    private double lon;
    private double height;
    private HashMap<String, String> attr = null;

    public OSMNode(int id) {
        super(id);
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

    public double getHeight() {
        return height;
    }

    public GeoPosition getGeoPosition() {
        return new GeoPosition(lat, lon);
    }

    /**
     * returns the link to the target node or null if no such link exists.
     * If dst.equals(this), also null is returned
     * 
     * @param dst
     * @return
     */
    public L getLinkTo(OSMNode dst) {
        if (dst.equals(this)) {
            return null;
        }
        for (OSMLink link : getLinks()) {
            if (link.getSource().equals(dst) || link.getTarget().equals(dst)) {
                return (L) link;
            }
        }
        return null;
    }

    /**
     * Set's a certain attribute key/value pair.
     * For both key and value, the intern representation is used to (hopefully)
     * save memory.
     *
     * @param key
     * @param value
     * @see String#intern()
     */
    public void setAttr(String key, String value) {
        if (attr == null) {
            attr = new HashMap<String, String>(1);
        }
        this.attr.put(key.intern(), value.intern());
    }

    /**
     * returns the map of attributes. The map is immutable if it is the empty map.
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
}
