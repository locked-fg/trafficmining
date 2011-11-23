package de.lmu.ifi.dbs.paros.algorithms.skyline;

import de.lmu.ifi.dbs.paros.graph.Path;
import de.lmu.ifi.dbs.utilities.MutablePriorityObject;

@Deprecated
public class PriorityPath<P extends Path> implements MutablePriorityObject<P> {

    private float prio;
    private final P path;

    public PriorityPath(float p, P pa) {
        prio = p;
        path = pa;
    }

    @Override
    public Comparable getKey() {
        return path.getLast().getName();
    }

    @Override
    public double getPriority() {
        return prio;
    }

    @Override
    public void setPriority(double newPriority) {
        prio = (float) newPriority;
    }

    public P getPath() {
        return path;
    }

    @Override
    public P getValue() {
        return path;
    }
}
