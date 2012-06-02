package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Link<N extends Node> {

    private int id = Integer.MIN_VALUE; // this id need not be unique!
    private static final Logger log = Logger.getLogger(Link.class.getName());
    // The length of the link in meters (regarding sub nodes and elevation data
    private double length;
    // total ascend of the link in meters (regarding sub nodes and elevation data
    private double ascend;
    // total descend of the link in meters (regarding sub nodes and elevation data
    private double descend;
    private int speed;
    private HashMap<String, String> attr = null;
    private List<N> ns = null;
    private final N target;
    private final N source;
    private final boolean oneway;

    /**
     * @param src
     * @param dst
     */
    public Link(N src, N dst) {
        this(src, dst, true);
    }

    public Link(N src, N dst, boolean oneWayOnly) {
        target = dst;
        source = src;
        oneway = oneWayOnly;
        src.addLink(this);
        dst.addLink(this);
    }

    public void addNodes(List<N> list) {
        for (N n : list) {
            addNodes(n);
        }
        if (ns instanceof ArrayList) { // don't make the array larger than it has to be
            ((ArrayList) ns).trimToSize();
        }
    }

    public void addNodes(N n) {
        if (n == null) {
            throw new NullPointerException("no null nodes allowed");
        }
        if (ns == null) {
            ns = new ArrayList<>(2);
        }
        if (ns.isEmpty() && (!n.equals(getSource()) && !n.equals(getTarget()))) {
            log.info("initializing sublist with a node which is neither start nor target node");
        }
        ns.add(n);
    }

    /**
     * Sets a certain attribute key/value pair.
     *
     * @param key
     * @param value
     */
    public void setAttr(String key, String value) {
        if (attr == null) {
            attr = new HashMap<>(1);
        }
        this.attr.put(key.intern(), value.intern());
    }

    /**
     * Sets the length of this link. This value should regard all subnodes and
     * if possible elevation data.
     *
     * @param length the length measured in meters
     */
    public void setLength(double length) {
        this.length = length;
    }

    /**
     * The length of the link in meters.
     *
     * @return length in meters
     */
    public double getLength() {
        return this.length;
    }

    public void setAscend(double ascend) {
        this.ascend = ascend;
    }

    public void setDescend(double descend) {
        this.descend = descend;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    /**
     * Returns an immutable list of detailed nodes incl. start and end node.
     *
     * @return list of nodes or empty list
     */
    public List<N> getNodes() {
        if (ns == null) {
            log.log(Level.SEVERE, "WARNING! link {0} does not contain any nodes - returning empty list", id);
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList(ns);
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

    public double getAscend() {
        return ascend;
    }

    public double getDescend() {
        return descend;
    }

    public int getSpeed() {
        return speed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (this.id > 0) {
            throw new IllegalStateException("Reassiging ID from link " + this + " to " + id);
        }
        this.id = id;
    }

    @Override
    public String toString() {
        return "id: " + id + ", " + super.toString();
    }

    public boolean isOneway() {
        return oneway;
    }

    /**
     * Returns the Node that was defined as the target in the constructor.
     *
     * @return
     */
    public N getSource() {
        return source;
    }

    /**
     * Returns the node that was defined as the target in the constructor.
     *
     * @return
     */
    public N getTarget() {
        return target;
    }

    /**
     * Returns the "other side" of the link. Belonging to the direction from
     * where you enter a link, either side may be the target. The ONEWAY flag
     * has NO impact on this method.
     *
     * @param N one side of the Link
     * @return the other side (node) of the link
     * @throws IllegalArgumentException if start is neither src nor dst
     */
    public N getTarget(N start) {
        if (!start.equals(this.source) && !start.equals(target)) {
            log.log(Level.FINE, "In: {0} but link is from {1} to {2}", new Object[]{start.toString(), source.toString(), target.toString()});
            throw new IllegalArgumentException("The given node is neither start nor the end of this link.");
        }
        return start.equals(source) ? target : source;
    }
}
