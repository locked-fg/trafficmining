package de.lmu.ifi.dbs.paros.noOsm;

import de.lmu.ifi.dbs.utilities.MutablePriorityObject;

@Deprecated
public class PrioritySkyline implements MutablePriorityObject {

    float prio;
    SubSkyline skyline;

    public PrioritySkyline(float p, SubSkyline ssky) {
        prio = p;
        skyline = ssky;

    }

    public SubSkyline getSkyline() {
        return skyline;
    }

    @Override
    public Comparable getKey() {
        return skyline.end.getName();
    }

    @Override
    public double getPriority() {
        return prio;
    }

    @Override
    public Object getValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPriority(double newPriority) {
        this.setPriority(newPriority);
    }
}
