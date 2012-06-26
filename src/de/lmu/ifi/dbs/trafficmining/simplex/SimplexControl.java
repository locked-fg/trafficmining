package de.lmu.ifi.dbs.trafficmining.simplex;

import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import java.util.Collection;
import java.util.List;
import javax.crypto.spec.PSource;

public interface SimplexControl<T extends PointSource> {

    public void setPoints(Collection<T> ps);

    public void setHighlight(Collection<T> ps);

    public void setAttributNames(List<String> names);

    @Deprecated
    public PointPanel getPointPanel();

    /**
     * in 1D,2D the eventSource will be the same as the returned point, in 3D,
     * the event source is 2D, but actually you'd like to have the 3D point source,
     * that you set into the control.
     *
     * @param eventSource
     * @return the corresponding point or null if the point is not in the panel
     */
    public T getSourceFor(T eventSource);

    public List<T> getSourceFor(List<T> eventSource);
}
