package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Node<L extends Link> {

    private static final Logger log = Logger.getLogger(Node.class.getName());
    private final int id;
    private List<L> links;

    public Node(int id) {
        this.id = id;
        this.links = new ArrayList<>(1);
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

    @Override
    public String toString() {
        return "N" + id;
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
