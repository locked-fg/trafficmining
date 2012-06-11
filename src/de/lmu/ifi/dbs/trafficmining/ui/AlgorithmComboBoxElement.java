package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import java.io.File;
import java.util.logging.Logger;

/**
 * Element in a combobox that represents an algorithm
 *
 * @author graf
 */
public class AlgorithmComboBoxElement implements Comparable<AlgorithmComboBoxElement> {

    private final static Logger logger = Logger.getLogger(AlgorithmComboBoxElement.class.getName());
    private final String name;
    private final Class<Algorithm> algorithm;

    public AlgorithmComboBoxElement(File jar, Class<Algorithm> key) throws InstantiationException, IllegalAccessException {
        this.name = jar.getName() + ": " + key.newInstance().getName();
        this.algorithm = key;
    }

    public Class<Algorithm> getAlgorithm() {
        return algorithm;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(AlgorithmComboBoxElement o) {
        return name.compareTo(o.name);
    }
}
