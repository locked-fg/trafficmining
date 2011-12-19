package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.result.AbstractResult;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.util.Arrays;
import java.util.List;

public class Simplex1Result extends AbstractResult {

    /**
     * @param path
     * @param weight Weight might be cost according to time or length
     */
    public void addResult(Path path, double weight) {
        addResult(path, new double[]{weight});
    }

    @Override
    public void setUnits(String... unitStrings) {
        if (unitStrings == null || unitStrings.length != 1) {
            throw new IllegalArgumentException("only 1 unit allowed");
        }
        units.clear();
        units.addAll(Arrays.asList(unitStrings));
    }
    
        @Override
    public void setAttributes(String... attribStrings) {
        if (attribStrings == null || attribStrings.length != 1) {
           throw new IllegalArgumentException("only 1 units allowed");
        }
        attribs.clear();
        attribs.addAll(Arrays.asList(attribStrings));
    }

}