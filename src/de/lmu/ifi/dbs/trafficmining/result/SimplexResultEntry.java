package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.awt.geom.Point2D;
import java.util.Locale;
import java.util.Vector;

public class SimplexResultEntry implements PointSource {

    private final double[] costRelative;
    private final int id;
    private final int dimensionality;

    public SimplexResultEntry(Result result, Path path, double[] costRelative, double[] costAbsolute, int id, int dimensionality) {
        assert Arrays2.findNaN(costRelative) < 0 : "NaN found in costRelative: " + Arrays2.join(costRelative, "|");
        assert Arrays2.findNaN(costAbsolute) < 0 : "NaN found in costAbsolute: " + Arrays2.join(costAbsolute, "|");
        this.costRelative = costRelative.clone();
        this.id = id;
        this.dimensionality = dimensionality;
    }

    public int getId() {
        return id;
    }

    @Override
    public Point2D getLocation() {
        if (dimensionality == 1) {
            return new Point2D.Double(costRelative[0], 0);
        } else if (dimensionality == 2) {
            return new Point2D.Double(costRelative[0], costRelative[1]);
        } else {
            throw new UnsupportedOperationException("Unsupported cost size = " + dimensionality);
        }
    }

    @Override
    public double[] getCoordinates() {
        return costRelative;
    }
}