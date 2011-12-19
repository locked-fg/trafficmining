package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * SwingWorker that starts the algorithm and waits for its termination.
 */
public class AlgorithmWorker extends SwingWorker<AlgorithmResult, Void> {

    private final static Logger log = Logger.getLogger(AlgorithmWorker.class.getName());
    private final Algorithm algorithm;

    AlgorithmWorker(Algorithm alg) {
        this.algorithm = alg;
    }

    @Override
    protected AlgorithmResult doInBackground() throws Exception {
        log.info("start algorithm");
        if (algorithm != null) {
            algorithm.run();
        }
        return new AlgorithmResult(algorithm.getResult(), algorithm.getStatistics());
    }
}