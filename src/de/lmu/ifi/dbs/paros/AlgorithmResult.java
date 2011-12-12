package de.lmu.ifi.dbs.paros;

import de.lmu.ifi.dbs.paros.result.Result;

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

    public Result getResult() {
        return result;
    }

    public Statistics getStatistics() {
        return stats;
    }
}
