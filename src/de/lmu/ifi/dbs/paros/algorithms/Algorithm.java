package de.lmu.ifi.dbs.paros.algorithms;

import de.lmu.ifi.dbs.paros.graph.Graph;
import de.lmu.ifi.dbs.paros.graph.Node;
import de.lmu.ifi.dbs.paros.graph.Path;
import de.lmu.ifi.dbs.paros.result.Result;
import de.lmu.ifi.dbs.paros.Statistics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Base class for algorithms
 *
 * DO NOT keep static information in implementing classes!
 * And check Thread.interrupted() for cancelation of the task
 * 
 * @author graf
 * @param <N>
 * @param <G>
 * @param <P>
 */
public abstract class Algorithm<N extends Node, G extends Graph, P extends Path>
        implements Runnable {

    private static final String STAT_PATHS = "# of paths";
    private static final String STAT_SUM_NODES = "# of nodes in paths";
    // -
    /** List of nodes that the algorithm should traverse */
    private List<N> nodes;
    /** The graph, on which the algorithm is operating*/
    private G graph;
    /** Statistics Container where all the stats should be put */
    private Statistics statistics = new Statistics();

    /**
     * Counts the number of unique nodes in the given paths
     * @param list
     * @return number of unique nodes
     */
    private int nodesInPaths(List<? extends Path> list) {
        HashSet set = new HashSet();
        for (Path path : list) {
            set.addAll(path.getNodes());
        }
        return set.size();
    }

    /**
     * delegates to {@link #buildStatistics(java.util.List)}
     * @param path
     */
    protected void buildStatistics(Path path) {
        List<Path> list = new ArrayList<Path>(1);
        list.add(path);
        buildStatistics(list);
    }

    /**
     * calculate some base-statistics
     * @param resultList
     */
    protected void buildStatistics(List<? extends Path> resultList) {
        if (resultList == null) {
            throw new NullPointerException("resultList must not be null");
        }

        int totalNodes = getGraph().getNodes().size();
        int nodesInPath = nodesInPaths(resultList);
        statistics.put(STAT_SUM_NODES, String.format(Locale.US, "%d / %d = %s%%", nodesInPath, totalNodes, nodesInPath * 100 / totalNodes));
        statistics.put(STAT_PATHS, Integer.toString(resultList.size()));
    }

    /**
     * Returns the name of the algorithm as it should be shown in the GUI.
     * If the Method is not overwritten, getClass().getName() is used to give at
     * least a hint about the algorithm. 
     * 
     * Nevertheless it is strongly recommended to override this method.
     *
     * @return name of the algorithm as it should be shown in the GUI
     */
    public String getName() {
        return getClass().getName();
    }

    /**
     * Returns a statistics object that contains all relevant information from this algorithm.
     *
     * @see Statistics
     * @return
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * Retrieve the result of the computation. This should currently be one of Simplex1-3Result.
     * 
     * @return a subclass of result or null if the task was cancelled
     */
    public abstract Result getResult();

    /**
     * Implement this method as the body of the algorithm. This method is called
     * from the gui and should do the main operations (like searching and finding
     * a path from start to end).
     *
     * Startnode, endnode and graph will be present when this method is called.
     */
    @Override
    public abstract void run();

    /**
     * Sets all nodes. The method is called by the gui
     * when starting the algorithm
     * 
     * @see #getNodes()
     * @param nodes 
     */
    public void setNodes(List<N> nodes) {
        this.nodes = nodes;
    }

    /**
     * Returns all nodes
     * 
     * @see #getNodes()
     * @return
     */
    public List<N> getNodes() {
        return nodes;
    }

    /**
     * return the graph that was used in the algorithm.
     *
     * @see #setGraph()
     * @return
     */
    public G getGraph() {
        return graph;
    }

    /**
     * sets the graph that the algorithm should be operating on. The method is
     * called by the gui when starting the algorithm.
     *
     * @see #getGraph() 
     * @param graph
     */
    public void setGraph(G graph) {
        this.graph = graph;
    }
}
