package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.result.AbstractResult;
import java.util.Arrays;


public class Simplex2Result extends AbstractResult {

    @Override
    public void setUnits(String... unitStrings) {
        if (unitStrings == null || unitStrings.length != 2) {
            throw new IllegalArgumentException("only 2 units allowed");
        }
        units.clear();
        units.addAll(Arrays.asList(unitStrings));
    }
    
        @Override
    public void setAttributes(String... attribStrings) {
        if (attribStrings == null || attribStrings.length != 2) {
           throw new IllegalArgumentException("only 2 units allowed");
        }
        attribs.clear();
        attribs.addAll(Arrays.asList(attribStrings));
    }
}
