package de.lmu.ifi.dbs.paros.noOsm;

import de.lmu.ifi.dbs.utilities.PriorityObject;

@Deprecated
public class SkylinePath implements PriorityObject<ComplexPath> {

    private float prio;
    private ComplexPath path;

    public SkylinePath(float p, ComplexPath pa) {
        prio = p;
        path = pa;

    }

    @Override
    public Comparable getKey() {
        return path.generateKey();
    }

    @Override
    public double getPriority() {
        return prio;
    }

    @Override
    public ComplexPath getValue() {
        return path;
    }
}
