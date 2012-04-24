package de.lmu.ifi.dbs.trafficmining.simplex;

import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import java.util.Collection;
import java.util.List;

public interface SimplexControl {

    public void setPoints(Collection<PointSource> ps);

    public void setHighlight(Collection<PointSource> ps);

    public void setAttributNames(List<String> names);

    public PointPanel getPointPanel();

    /**
     * in 1D,2D the eventSource will be the same as the returned point, in 3D,
     * the eventsource is 2D, but actually you'd like to have the 3D pointsource,
     * that you set into the control.
     * 
     * @param eventSource
     * @return the corresponding point or null if the point is not in the panel
     */
    public PointSource getSourceFor(PointSource eventSource);

    public List<PointSource> getSourceFor(List<PointSource> eventSource);
}
