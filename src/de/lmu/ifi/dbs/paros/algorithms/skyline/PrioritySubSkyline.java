/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.paros.algorithms.skyline;

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