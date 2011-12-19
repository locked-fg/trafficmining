package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.logging.Logger;

public class Link<N extends Node> {

    private static final Logger log = Logger.getLogger(Link.class.getName());
    private final N target;
    private final N source;
    private final boolean oneway;

    /**
     * @FIXME default = oneway? does this make sense?!
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
            log.fine("In: " + start.toString() + " but link is from " + source.toString() + " to " + target.toString());
            throw new IllegalArgumentException("The given node is neither start nor the end of this link.");
        }

        return start.equals(source) ? target : source;
    }

    @Override
    public String toString() {
        String s = source == null ? "null" : source.toString();
        s += " - ";
        s += target == null ? "null" : target.toString();
        s += ". Oneway: " + oneway;
        return s;
    }
}
