package de.lmu.ifi.dbs.paros.simplex;

import java.awt.geom.Point2D;

public class SimplexPoint3d implements PointPanel.PointSource {

    public final double x;
    public final double y;
    public final double z;

    /**
     * @param x
     * @param y
     * @param z
     */
    public SimplexPoint3d(double x, double y, double z) {
        this.y = y;
        this.z = z;
        this.x = x;
    }

    @Override
    public String toString() {
        return String.format("%.3f |%.3f |%.3f", x, y, z);
    }

    @Override
    public Point2D getLocation() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public double[] getCoordinates() {
        return new double[]{x, y, z};
    }
}
