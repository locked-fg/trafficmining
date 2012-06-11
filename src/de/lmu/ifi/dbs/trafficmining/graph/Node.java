package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class Node<L extends Link> {

    private final int id;
    private double lat;
    private double lon;
    private double height = Double.NaN;
    @Deprecated
    private String name = null;
    private HashMap<String, String> attr = null;
    private List<L> links = new ArrayList<>(1);

    public Node(int id) {
        this.id = id;
    }

    @Deprecated
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
        return "[node id: " + id + ",lat/lon: " + lat + "," + lon + "]";
    }

    public List<L> getOutLinks() {
        List<L> out = new ArrayList<>(links.size());
        for (L link : links) {
            if (!link.isOneway() || link.getSource().equals(this)) {
                out.add(link);
            }
        }
        return out;
    }

    public List<L> getInLinks() {
        List<L> out = new ArrayList<>(links.size());
        for (L link : links) {
            if (!link.isOneway() || link.getTarget().equals(this)) {
                out.add(link);
            }
        }
        return out;
    }

    public List<L> getLinks() {
        return links;
    }

    public void setLinks(List<L> links) {
        this.links = links;
    }

    public void addLink(L link) {
        if (!links.contains(link)) {
            links.add(link);
        }
    }

    public void removeLink(L link) {
        links.remove(link);
    }

    public int getName() {
        return id;
    }

    public int getDegree() {
        return links.size();
    }

    /**
     * Returns all links to the target node. If dst.equals(this), an empty list
     * is returned
     *
     * @param dst
     * @return list of links to the target node
     */
    public List<L> getLinksTo(Node dst) {
        List<L> result = new ArrayList<>();
        if (dst.equals(this)) {
            return result;
        }
        for (L link : getLinks()) {
            if (link.getSource().equals(dst) || link.getTarget().equals(dst)) {
                result.add(link);
            }
        }
        return result;
    }
}
