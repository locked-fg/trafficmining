package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OSMLink<N extends OSMNode> extends Link<N> {

    private int id = -666; // this id need not be unique!
    private static final Logger log = Logger.getLogger(OSMLink.class.getName());
    /**
     * The length of the link in meters (regarding sub nodes and elevation data
     * if possible).
     */
    private double length;
    private double ascend;
    private double descend;
    private int speed;
    private HashMap<String, String> attr = null;
    private List<N> ns = null;

//    public OSMLink(N src, N dest) {
//        super(src, dest);
//    }

    public OSMLink(N src, N dest, boolean oneway) {
        super(src, dest, oneway);
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

    public void delNodes(int i) {
        if (i >= ns.size() || i < 0) {
            throw new NullPointerException("node index wrong");
        } else {
            ns.remove(i);
        }
    }

    public void delNodes(N node) {
        ns.remove(node);
    }

    public void clearNodes() {
        ns.clear();
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
     * Sets the length of this link. This value should regard all subnodes and
     * if possible elevation data.
     *
     * @param length the length measured in m
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
     * Returns an immutable list of detailed nodes.
     *
     * @return
     */
    public List<N> getNodes() {
        if (ns==null) {
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
}
