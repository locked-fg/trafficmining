package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic statistics class which can be extended to hold any other information.
 * Each single information should be accessible by a getter in order to be
 * displayed after the execution.
 *
 * @author graf
 */
public class Statistics {

    private static final Logger log = Logger.getLogger(Statistics.class.getName());
    /** tag for path specific stats: amount of traffic lights */
    public static final String STAT_PATH_TRAFFIC_SIGNALS = "Trafficlights";
    /** tag for path specific stats: amount of nodes with a degree > 2 */
    public static final String STAT_PATH_NODES_DEG_GT2 = "Crossing nodes (degree > 2)";
    /**
     * do not keep more than that many nodes
     */
    public static final int MAX_VISITED_NODES = 500000;
    /**
     * General statistics about teh algorithm like runtime etc.
     */
    private HashMap<String, String> map = new HashMap<>();
    /**
     * Statistics about a certain path
     */
    private HashMap<Path, Map<String, String>> pathMap = new HashMap<>();
    /**
     * List of visited nodes
     */
    private List<Node> visitedNodes = new ArrayList<>();

    public List<Node> getVisitedNodes() {
        return visitedNodes;
    }

    /**
     * saves the list of visited nodes. The list is limited to a maximum of
     * {@link #MAX_VISITED_NODES}
     *
     * @param nodes
     * @see #MAX_VISITED_NODES
     */
    public void setVisitedNodes(Collection<? extends Node> nodes) {
        this.visitedNodes = new ArrayList<>();
        if (nodes.size() > MAX_VISITED_NODES) {
            log.log(Level.INFO, "{0} visited nodes were requested to be stored. Storing only the first {1}", new Object[]{nodes.size(), MAX_VISITED_NODES});
            for (Iterator<? extends Node> it = nodes.iterator(); this.visitedNodes.size() < MAX_VISITED_NODES;) {
                this.visitedNodes.add(it.next());
            }
        } else {
            this.visitedNodes.addAll(nodes);
        }
    }

    /**
     * Add general statistical information about the search (not per path)
     *
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        map.put(key, value);
    }

    public String get(String key) {
        return map.get(key);
    }

    public void putPath(Path key, Map<String, String> pathInfos) {
        pathMap.put(key, pathInfos);
    }

    public Map<String, String> getPath(Path p) {
        if (!pathMap.containsKey(p)) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(pathMap.get(p));
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        List<String> keys = new ArrayList<>();
        keys.addAll(map.keySet());
        StringBuilder out = new StringBuilder(100);
        for (String key : keys) {
            out.append(key).append(" : ").append(get(key)).append(" | ");
        }
        return out.toString();
    }
}
