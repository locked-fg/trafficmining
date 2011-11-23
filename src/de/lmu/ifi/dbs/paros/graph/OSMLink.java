package de.lmu.ifi.dbs.paros.graph;

import java.util.*;
import java.util.logging.Logger;

public class OSMLink<N extends OSMNode> extends Link<N> {

    private int id = -1; // this id need not be unique!
    private static final Logger log = Logger.getLogger(OSMLink.class.getName());
    private double distance;
    private double ascend;
    private double descend;
    private int speed;
    private HashMap<String, String> attr = null;
    private List<N> ns = null;

    public OSMLink(N src, N dest) {
        super(src, dest);
    }

    public OSMLink(N src, N dest, boolean oneway) {
        super(src, dest, oneway);
    }

    public void addNodes(List<N> list) {
        for (N n : list) {
            addNodes(n);
        }
        if (ns instanceof ArrayList) { // let don't make the array larger than it has to be
            ((ArrayList) ns).trimToSize();
        }
    }

    public void addNodes(N n) {
        if (n == null) {
            throw new NullPointerException("no null nodes allowed");
        }
        if (ns == null) {
            ns = new ArrayList<N>(3);
        }
        if (ns.size() == 0 && (!n.equals(getSource()) && !n.equals(getTarget()))) {
            log.info("initializing sublist with a node which is neither start nor target node");
        }
        ns.add(n);
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

    public void setDistance(double distance) {
        this.distance = distance;
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
     * Returns list of detailed nodes. The list is immutable!
     * @todo check performance impact (ns == null)?
     * @return
     */
    public List<N> getNodes() {
//        if (ns == null) {
//            ArrayList<N> ret = new ArrayList<N>(2);
//            ret.add(this.getSource());
//            ret.add(this.getTarget());
//            System.out.println("OSMLink - ID: "+id+" --> getNodes: "+count++);
//            return ret;
//        }
        return Collections.unmodifiableList(ns);
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

    public double getDistance() {
        return distance;
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
