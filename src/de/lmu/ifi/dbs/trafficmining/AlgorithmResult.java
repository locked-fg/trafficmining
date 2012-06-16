package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.result.Result;

/**
 * Class that wraps the compound result of the AlgorithmWorker.
 *
 * @author graf
 * @see AlgorithmWorker#get()
 */
public class AlgorithmResult {

    private final Result result;
    private final Statistics stats;

    public AlgorithmResult(Result result, Statistics stats) {
        this.result = result;
        this.stats = stats;
    }

    public AlgorithmResult() {
        result = null;
        stats = null;
    }

    public Result getResult() {
        return result;
    }

    public Statistics getStatistics() {
        return stats;
    }

    boolean isEmpty() {
        return result == null;
    }
}
