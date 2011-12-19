package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.util.List;
import java.util.Map;

public interface Result {

    /**
     * Returns a map of results where each path points to the corresponding costs.
     * In a simple Dijkstra shortest path, this would be for example "path1, 4.5 (km)".
     * 
     * @return path to cost mapping of all computed paths
     */
    public Map<Path, double[]> getResults();

    /**
     * return the list of units, set by {@link #setUnits(strings)}
     *
     * @return
     * @see #setUnits(java.lang.String[])
     */
    public List<String> getUnits();
    
    
    /**
     * return the list of attributes
     * 
     * @return 
     */
    public List<String> getAttributes();
    
    public void setAttributes(String... attribs);

    /**
     * Sets a list of units for the costs in {@link #getResults()}.
     * For a shortest path, this would be a 1 element array containing "km". For
     * a fastest way "hours".
     *
     * If both distance and time are logged this might contain ["km","h"].
     * In that case, {@link #getResults()} must also map to two-elemental arrays
     *
     * If you have absolutely no cue about what you should enter here, just return
     * an array of the correct length, containing empty strings.
     *
     */
    public void setUnits(String... unitStrings);
}
