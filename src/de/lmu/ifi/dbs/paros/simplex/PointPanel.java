package de.lmu.ifi.dbs.paros.simplex;

import de.lmu.ifi.dbs.utilities.Math2;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

/**
 * PointPanel paints PointSource objects that are in the domain of [0-1] on this
 * panel. Points can become highlighted as well.
 *
 * @author graf
 */
public class PointPanel extends JXPanel {

    /**
     * Data object with values in [0,1]
     */
    public interface PointSource {

        Point2D getLocation();

        double[] getCoordinates();
    }
    // --
    private final Logger log = Logger.getLogger(PointPanel.class.getName());
    private HashSet<PointSource> points = new HashSet<PointSource>();
    private HashSet<PointSource> highlight = new HashSet<PointSource>();
    private int pointSize = 6; // pixel
    private Painter painter = new DefaultPainter();

    public PointPanel() {
        initComponents();
        setPointSize(pointSize);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (PointSource p : points) {
            if (!highlight.contains(p)) {
                // translate points in unit space to absolut values
                Point point = pointSourceToPaintCoordinates(p);
                // move the point coordintate to 0,0 and back after painting
                g.translate(point.x, point.y);
                painter.paint(g, p, -1, -1);
                g.translate(-point.x, -point.y);
            }
        }
        // paint highlighted points on top so that they are ALWAYS visible
        for (PointSource p : highlight) {
            // translate points in unit space to absolut values
            Point point = pointSourceToPaintCoordinates(p);
            // move the point coordintate to 0,0 and back after painting
            g.translate(point.x, point.y);
            painter.paint(g, p, -1, -1);
            g.translate(-point.x, -point.y);
        }
    }

    /**
     * get the k nearest points in the map
     * @param query querypoint in the coordinate system of the panel
     * @param k max amount of points being returned
     * @return list of max. k points being nearest to the query
     */
    public List<PointSource> getKnnPoints(Point query, double k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0 but was " + k);
        }

        // calculate distance to all points
        ArrayList<SimpleEntry<Double, PointSource>> list = new ArrayList<SimpleEntry<Double, PointSource>>();
        for (PointSource p2 : points) {
            Point p1 = pointSourceToPaintCoordinates(p2);
            double currDist = query.distance(p1);
            list.add(new SimpleEntry<Double, PointSource>(currDist, p2));
        }
        // sort list
        Collections.sort(list, new Comparator<SimpleEntry<Double, PointSource>>() {

            @Override
            public int compare(SimpleEntry<Double, PointSource> o1, SimpleEntry<Double, PointSource> o2) {
                return Double.compare(o1.getKey(), o2.getKey());
            }
        });
        // get top k and extract points allone
        ArrayList<PointSource> result = new ArrayList<PointSource>();
        for (int i = 0; i < k; i++) {
            result.add(list.get(i).getValue());
        }
        return result;
    }

    /**
     * get the k nearest points in the map
     * @param query querypoint in the coordinate system of the panel
     * @param k max distance
     * @return list of points with dist < epsilon
     */
    public List<PointSource> getEpsilonPoints(Point query, double epsilon) {
        if (epsilon < 0) {
            throw new IllegalArgumentException("epsilon must be >= 0 but was " + epsilon);
        }

        // calculate distance to all points
        ArrayList<PointSource> list = new ArrayList<PointSource>();
        for (PointSource p2 : points) {
            Point p1 = pointSourceToPaintCoordinates(p2);
            double currDist = query.distance(p1);
            if (currDist < epsilon) {
                list.add(p2);
            }
        }
        return list;
    }

    public Point pointSourceToPaintCoordinates(PointSource ps) {
        Insets in = getInsets();
        int w = getWidth() - in.left - in.right - pointSize - 1;
        int h = getHeight() - in.top - in.bottom - pointSize;

        Point2D p = ps.getLocation();
        double x = p.getX();
        // "1-y" because painting coordinate system is top down!
        double y = 1 - p.getY();

        // [0-1] to real coordinates
        double delta = pointSize / 2d;
        x = (int) (in.left + (x * w + delta));
        y = (int) (in.top + (y * h + delta));

        return new Point((int) x, (int) y);
    }

    public Painter getPainter() {
        return painter;
    }

    public void setPainter(Painter painter) {
        this.painter = painter;
    }

    // ------------------------- getter/setter here ----------------------------
    public int getPointSize() {
        return pointSize;
    }

    public void setPointSize(int pointSize) {
        this.pointSize = pointSize;
        setMinimumSize(new Dimension(pointSize + 4, pointSize + 4));
        repaint();
    }

    public HashSet<PointSource> getPoints() {
        return points;
    }

    public void setPoints(Collection<? extends PointSource> in) {
        assert in != null;
        points.clear();
        highlight.clear();

        for (PointSource ps : in) {
            Point2D p = ps.getLocation();
            if (!Math2.isIn(0, p.getX(), 1) || !Math2.isIn(0, p.getY(), 1)) {
                log.warning("Point out of bounds [0,1]: " + p.getX() + ":" + p.getY());
            }
            points.add(ps);
        }
        repaint();
    }

    public HashSet<PointSource> getHighlight() {
        return highlight;
    }

    public void setHighlight(Collection<? extends PointSource> in) {
        highlight.clear();
        for (PointSource p : in) {
            if (p != null && points.contains(p)) {
                highlight.add(p);
            }
        }
        repaint();
    }

    private class DefaultPainter implements Painter {

        private final Color highlightColor = Color.red;
        private final Color color = Color.black;

        @Override
        public void paint(Graphics2D g, Object object, int width, int height) {
            PointSource p = (PointSource) object;
            if (highlight.contains(p)) { // highlighted?
                g.setColor(highlightColor);
            } else {
                g.setColor(color);
            }
            // draw point
            g.fillOval(-pointSize / 2, -pointSize / 2, pointSize, pointSize);
            g.drawOval(-pointSize / 2, -pointSize / 2, pointSize, pointSize);
            // draw white circle with 1px distance from the border
            g.setColor(Color.WHITE);
            g.drawOval(1 - pointSize / 2, 1 - pointSize / 2, pointSize - 2, pointSize - 2);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setMinimumSize(new java.awt.Dimension(10, 100));
        setScrollableTracksViewportHeight(false);
        setScrollableTracksViewportWidth(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 140, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
