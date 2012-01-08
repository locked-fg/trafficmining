
package de.lmu.ifi.dbs.trafficmining.algorithms.skyline;

import de.lmu.ifi.dbs.utilities.PriorityObjectAdapter;


public class PrioritySubSkyline extends PriorityObjectAdapter<OSMSubSkyline> {

    public PrioritySubSkyline(OSMSubSkyline t, double priority) {
        super(t, priority);
    }

    @Override
    public Comparable getKey() {
        return getValue().getEnd().toString();
    }

    @Override
    public String toString() {
        return getKey() + " : " + getPriority();
    }
}