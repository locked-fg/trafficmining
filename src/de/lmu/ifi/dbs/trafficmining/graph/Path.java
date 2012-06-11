package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author graf
 * @param <P>
 * @param <N>
 * @param <L>
 */
public class Path<P extends Path, N extends Node, L extends Link> implements
        Iterable<N> {

    static final Logger log = Logger.getLogger(Path.class.getName());
    /**
     * The last node of this path.
     */
    private final N endNode;
    /**
     * The first node of the complete path hierarchy. The start node of this
     * path segment can ge obtained by {@link #getLocalStart()}
     */
    private final N startNode;
    /**
     * Reference pointer to the parent path segment or null if this is the first
     * segment in the path.
     */
    private final P previousPath;
    /**
     * The link that represents the current path segment or null if the path was
     * constructed just by defining nodes.
     */
    private final L link;
    /**
     * The amount of path segments from the first segment to this segment.
     */
    private final int hops;

    /**
     * Preferred method to initialize a path with the given link.
     *
     * @param parent
     * @param link
     */
    public Path(P parent, L link) {
        this.startNode = (N) parent.getFirst();
        this.endNode = (N) link.getTarget(parent.getLast());
        this.hops = parent.getLength() + 1;
        this.previousPath = parent;
        this.link = link;
    }

    /**
     * Generates a path between the given nodes.<br>
     *
     * If the nodes are connected by multible links, it will not be determinable
     * which link should be used!
     *
     * @see #Path(de.lmu.ifi.dbs.trafficmining.graph.Node,
     * de.lmu.ifi.dbs.trafficmining.graph.Link) as preferred method
     * @param start
     * @param end
     */
    public Path(N start, N end) {
        this(start, end, start.equals(end) ? 0 : 1, null);
    }

    /**
     * Consructs a new Path from start to end. If prev is null, you might want
     * to use Path(N start, N end) instead.
     *
     * If you extend an existing path, use Path(P p, L n) instead as the link
     * information will not be lost.
     *
     * @param start
     * @param end
     * @param hops
     * @param prev
     */
    public Path(N start, N end, int hops, P prev) {
        if (!start.getLinksTo(end).isEmpty()) {
            log.warning("A path between 2 nodes is to be created even though "
                    + "links would connect the nodes. This might indicate an error. "
                    + "Consider using Path(P parent, L link)");
        }

        this.startNode = start;
        this.endNode = end;
        this.hops = hops;
        this.previousPath = prev;
        this.link = null;
    }

    /**
     * Extends a path to the given node. If multiple links exist for the end of
     * the path and the given node, the painter WILL NOT be able to determine
     * which link was followed!
     *
     * {@link Path(de.lmu.ifi.dbs.trafficmining.graph.Path, de.lmu.ifi.dbs.trafficmining.graph.Link)}
     * shoule be preferred
     *
     * @see Path(de.lmu.ifi.dbs.trafficmining.graph.Path,
     * de.lmu.ifi.dbs.trafficmining.graph.Link)
     * @param p
     * @param n
     */
    public Path(P p, N n) {
        this((N) p.getFirst(), n, p.getLength() + 1, p);
    }

    /**
     * Initializes a path with a start node and a following link.
     *
     * @param startNode the start node
     * @param link the link that this path follows
     */
    public Path(N startNode, L link) {
        this.startNode = startNode;
        this.endNode = (N) link.getTarget(startNode);
        this.hops = 1;
        this.previousPath = null;
        this.link = link;
    }

    /**
     * Determines the length in term of path segments of this path hierarchy.
     *
     * @return amount of path segments on this hierarchy.
     */
    public int getLength() {
        return hops;
    }

    /**
     * Returns the first node of concatenated paths. If the start of this
     * segment needs to be obtained, use {@link #getLocalStart()}
     *
     * @return node
     */
    public N getFirst() {
        return startNode;
    }

    /**
     * Returns the start of this path segment (either the start node or the end
     * node of the parent path). Use {@link #getFirst()} in order to obtain the
     * first node of the complete path hierarchy.
     *
     * @return start node of this segment.
     */
    public N getLocalStart() {
        if (previousPath == null) {
            return startNode;
        } else {
            return (N) previousPath.getLast();
        }
    }

    /**
     * Return the end of this path segment.
     *
     * @return node
     */
    public N getLast() {
        return endNode;
    }

    /**
     * Returns the parent path object of this path
     *
     * @return previous path element
     */
    public P getParent() {
        return previousPath;
    }

    /**
     * Returns the link that belongs to this path. May be null!
     *
     * @return link object representing this path or null
     */
    public L getLink() {
        return link;
    }

    /**
     * Constructs a list of path segments of this hierarchy.
     *
     * @return path segment list starting with the first path at index 0
     */
    protected List<P> getPathSegments() {
        List<P> p = new ArrayList<>(getLength());
        P tmp = (P) this;
        while (tmp != null) {
            p.add(tmp);
            tmp = (P) tmp.getParent();
        }
        Collections.reverse(p);
        return p;
    }

    /**
     * Iterates through all parents of this path and gathers all Nodes returned
     * by getLast().
     *
     * Keep the case in mind where two nodes are connected by more than one
     * link!
     *
     * @return List of getLast() nodes starting with the startNode at position 0
     */
    public List<N> getParentNodes() {
        List<N> nodes = new ArrayList<>(hops);
        Path<P, N, L> p = this;
        while (p != null) {
            nodes.add(p.getLast());
            p = p.getParent();
        }
        nodes.add(startNode);
        Collections.reverse(nodes);
        return nodes;
    }

    /**
     * @param link
     * @return true if start AND endnode of a link are contained in the path
     */
    public boolean contains(Link<? extends Node> link) {
        return contains(link.getSource()) && contains(link.getTarget());
    }

    /**
     * checks whether this node is contained in this path hierarchy
     *
     * @param n the node to check
     * @return true if n is part of the path, false otherwise
     */
    public boolean contains(Node n) {
        if (getFirst().equals(n)) {
            return true;
        }

        Path p = this;
        while (p.getLength() > 1) {
            if (p.getLast().equals(n)) {
                return true;
            }
            p = p.getParent();
        }
        assert p.getLength() == 1 : "p.length not 1 but " + p.getLength();
        return p.getLast().equals(n);
    }

    @Override
    public String toString() {
        List<N> nodes = getParentNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(nodes.get(i).toString());
            if (i < nodes.size() - 1) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    /**
     * provides an iterator over all the nodes of this path, starting from the
     * start node
     *
     * @return node iterator
     */
    @Override
    public Iterator<N> iterator() {
        return getParentNodes().iterator();
    }
}
