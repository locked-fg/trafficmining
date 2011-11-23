package de.lmu.ifi.dbs.paros.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author graf
 * @param <P>
 * @param <N>
 * @param <L>
 */
public class Path<P extends Path, N extends Node, L extends Link> implements
        Iterable<N> {

    private final N end;
    private final N start;
    private final P prev;
    private final L link;
    private final int hops;

    /**
     * Preferred method to initialize a path with the given link.
     * 
     * @param parent
     * @param link 
     */
    public Path(P parent, L link) {
        this.start = (N) parent.getFirst();
        this.end = (N) link.getTarget(parent.getLast());
        this.hops = parent.getLength() + 1;
        this.prev = parent;
        this.link = link;
    }

    /**
     * @see #Path(de.lmu.ifi.dbs.paros.graph.Node, de.lmu.ifi.dbs.paros.graph.Link) as preferred method
     * @param start
     * @param end 
     */
    public Path(N start, N end) {
        this.start = start;
        this.end = end;
        if (start.equals(end)) {
            this.hops = 0;
        } else {
            this.hops = 1;
        }
        this.prev = null;
        this.link = null;
    }

    /**
     * Consructs a new Path from start to end.
     * If prev is null, you might want to use Path(N start, N end) instead.
     *
     * If you extend an existing path, use Path(P p, L n) instead. as the Link 
     * information will not be lost.
     *
     * @param start 
     * @param end
     * @param hops
     * @param prev
     */
    public Path(N start, N end, int hops, P prev) {
        this.start = start;
        this.end = end;
        this.hops = hops;
        this.prev = prev;
        this.link = null;
    }

    /**
     * Extends a path to the given node. If multiple links exist for the end of 
     * the path and the given node, the painter WILL NOT be able to determine which 
     * link was followed!
     * 
     * {@link Path(de.lmu.ifi.dbs.paros.graph.Path, de.lmu.ifi.dbs.paros.graph.Link)} shoule be preferred
     * 
     * @see Path(de.lmu.ifi.dbs.paros.graph.Path, de.lmu.ifi.dbs.paros.graph.Link)
     * @param p
     * @param n 
     */
    public Path(P p, N n) {
        this.start = (N) p.getFirst();
        this.end = n;
        this.hops = p.getLength() + 1;
        this.prev = p;
        this.link = null;
    }

    /**
     * Initializes a path with a start node and a following link.
     * 
     * @param startNode the start node
     * @param link the link that this path follows
     */
    public Path(N startNode, L link) {
        this.start = startNode;
        this.end = (N) link.getTarget(start);
        this.hops = 1;
        this.prev = null;
        this.link = link;
    }

    public int getLength() {
        return hops;
    }

    public N getFirst() {
        return start;
    }

    public N getLast() {
        return end;
    }

    public P getParent() {
        return prev;
    }

    public L getLink() {
        return link;
    }

    protected List<P> getPathSegments() {
        List<P> p = new ArrayList<P>();
        P tmp = (P) this;
        while (tmp != null) {
            p.add(tmp);
            tmp = (P) tmp.getParent();
        }
        Collections.reverse(p);
        return p;
    }

    /**
     * @return List of nodes from begin to end ignoring the links!
     */
    public List<N> getNodes() {
        List<N> nodes = new ArrayList<N>(hops);
        Path<P, N, L> p = this;
        while (p != null) {
            nodes.add(p.getLast());
            p = p.getParent();
        }
        nodes.add(start);
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
        List<N> nodes = getNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(nodes.get(i).toString());
            if (i < nodes.size() - 1) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    @Override
    public Iterator<N> iterator() {
        return getNodes().iterator();
    }
}
