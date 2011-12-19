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
        this.links = new ArrayList<L>(1);
    }

    public List<L> getOutLinks() {
        List<L> out = new ArrayList<L>(links.size());
        for (L link : links) {
            if (!link.isOneway() || link.getSource().equals(this)) {
                out.add(link);
            }
        }
        return out;
    }

    public List<L> getInLinks() {
        List<L> out = new ArrayList<L>(links.size());
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
}
