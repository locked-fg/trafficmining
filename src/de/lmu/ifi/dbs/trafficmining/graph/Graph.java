package de.lmu.ifi.dbs.trafficmining.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Graph<N extends Node, L extends Link> {

    private HashMap<Integer, N> nodes = new HashMap<>();

    public Graph() {
    }

    public void addNode(N node) {
        nodes.put(node.getName(), node);
    }
    
    public void addNodeList(List<N> nodeList) {
        for (N n : nodeList) {
            nodes.put(n.getName(), n);
        }
    }

    public void removeNode(N node) {
        nodes.remove(node.getName());
    }

    public void removeNode(int id) {
        nodes.remove(id);
    }

    public N getNode(int name) {
        return nodes.get(name);
    }

    public Collection<N> getNodes() {
        return nodes.values();
    }

    public int nodeCount() {
        return nodes.size();
    }
}
