package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.util.*;
import java.util.logging.Logger;

public abstract class AbstractResult implements Result {

    private static final Logger log = Logger.getLogger(AbstractResult.class.getName());
    private final HashMap<Path, double[]> results = new HashMap<>();
    protected final List<String> units = new ArrayList<>(3);
    protected final List<String> attribs = new ArrayList<>(3);

    @Override
    public Map<Path, double[]> getResults() {
        return results;
    }

    /**
     * @param path
     * @param weight Weight might be cost according to time or length
     */
    public void addResult(Path path, double[] weight) {
        if (path != null) {
            getResults().put(path, weight);
        } else {
            log.warning("tried to set null-path");
        }
    }

    public void clearAllLists() {
        results.clear();
        units.clear();
    }

    /**
     * @param path
     * @param weight Weight might be cost according to time or length
     */
    public void addResult(Path path, float[] weight) {
        addResult(path, Arrays2.convertToDouble(weight));
    }

    public Collection<Path> getPaths() {
        return results.keySet();
    }

    public double[] getWeight(Path key) {
        return results.get(key);
    }

    @Override
    public List<String> getUnits() {
        return Collections.unmodifiableList(units);
    }

    @Override
    public List<String> getAttributes() {
        return Collections.unmodifiableList(attribs);
    }
}
