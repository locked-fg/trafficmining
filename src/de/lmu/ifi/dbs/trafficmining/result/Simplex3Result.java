package de.lmu.ifi.dbs.trafficmining.result;

import java.util.Arrays;


public class Simplex3Result extends AbstractResult {

    @Override
    public void setUnits(String... unitStrings) {
        if (unitStrings == null || unitStrings.length != 3) {
            throw new IllegalArgumentException("only 3 units allowed");
        }
        units.clear();
        units.addAll(Arrays.asList(unitStrings));
    }

    @Override
    public void setAttributes(String... attribStrings) {
        if (attribStrings == null || attribStrings.length != 3) {
           throw new IllegalArgumentException("only 3 units allowed");
        }
        attribs.clear();
        attribs.addAll(Arrays.asList(attribStrings));
    }
}
