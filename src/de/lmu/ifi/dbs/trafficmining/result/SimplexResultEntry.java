/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.awt.geom.Point2D;
import java.util.Locale;
import java.util.Vector;


public class SimplexResultEntry implements PointSource {

    private final Result result;
    private final double[] costRelative;
    private final double[] costAbsolute;
    private final int id;
    private final Path path;
    private final int dimensionality;

    public SimplexResultEntry(Result result, Path path, double[] costRelative, double[] costAbsolute, int id, int dimensionality) {
        assert Arrays2.findNaN(costRelative) < 0 : "NaN found in costRelative: " + Arrays2.join(costRelative, "|");
        assert Arrays2.findNaN(costAbsolute) < 0 : "NaN found in costAbsolute: " + Arrays2.join(costAbsolute, "|");
        this.result = result;
        this.costRelative = costRelative;
        this.costAbsolute = costAbsolute;
        this.id = id;
        this.path = path;
        this.dimensionality = dimensionality;
    }

    public Vector getVector() {
        Vector vec = new Vector();
        vec.add(id);
        for (int i = 0; i < costRelative.length; i++) {
            String unit = "";
            if (result.getUnits().size() > i) {
                unit = result.getUnits().get(i);
            }
            vec.add(String.format(Locale.US, "%.4f %s", costAbsolute[i], unit));
        }
        return vec;
    }

    public double[] getCost() {
        return costRelative;
    }

    public int getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }

    public Result getResult() {
        return result;
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

    @Override
    public String toString() {
        String s = id + ": ";
        for (int i = 0; i < costRelative.length; i++) {
            String unit = "";
            if (result.getUnits().size() > i) {
                unit = result.getUnits().get(i);
            }
            s += String.format("%.4f %s | ", costAbsolute[i], unit);
        }
        return s;
    }
}