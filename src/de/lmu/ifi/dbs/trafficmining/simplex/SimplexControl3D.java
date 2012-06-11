package de.lmu.ifi.dbs.trafficmining.simplex;

import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.utilities.Arrays2;
import de.lmu.ifi.dbs.utilities.Math2;
import de.lmu.ifi.dbs.utilities.Vectors;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.logging.Logger;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

public class SimplexControl3D extends JXPanel implements SimplexControl {

    private static final Logger log = Logger.getLogger(SimplexControl3D.class.getName());
//    private final Distance distance = new EuclideanSquared();
    private Dimension formerDimension = null; // indicates resize of the panel
//    private Color coDomainBackground = new Color(220, 220, 220); // bg of the rounded triangle
    // -
    private HashMap<PointSource, MappedPoint> points = new HashMap<>();
    private HashMap<MappedPoint, PointSource> pointsBack = new HashMap<>();
    private HashMap<PointSource, MappedPoint> highlight = new HashMap<>();
    // -
    // A,B on y = 0 is uncool because zero weights of C cause negative coordinates
    // final double a_x = 0;
    // final double a_y = 0;
    // final double b_x = 1;
    // final double b_y = 0;
    // final double c_x = 0.5;
    // final double c_y = 0.5 * Math.sqrt(3);
    // -
    // C in bottomline on y = 0
    // A: top left, B top right
//    final double a_x = 0;
//    final double a_y = 0.5 * Math.sqrt(3);
//    final double b_x = 1;
//    final double b_y = 0.5 * Math.sqrt(3);
//    final double c_x = 0.5;
//    final double c_y = 0;
    // ----
    double scaleX = 1, scaleY = 1, transX = 0, transY = 0;

    /** Creates new form SimplexControl3D */
    public SimplexControl3D() {
        initComponents();
        pointPanel.setBackgroundPainter(new Simplex3DBackgroundpainter());

        { // calculate the extrema in order to set scale and translate components
            // this COULD be done analytically but it's quicker now to just calculate it by hand
            double[] a = map3Dto2D(new double[]{0, 0, 1});
            double[] b = map3Dto2D(new double[]{0, 1, 0});
            double[] c = map3Dto2D(new double[]{1, 0, 0});

            double minX = Math.min(a[0], Math.min(b[0], c[0])) - 0.001;
            double maxX = Math.max(a[0], Math.max(b[0], c[0])) + 0.001;

            double minY = Math.min(a[1], Math.min(b[1], c[1])) - 0.001;
            double maxY = Math.max(a[1], Math.max(b[1], c[1])) + 0.001;

            scaleX = 1 / (maxX - minX);
            scaleY = 1 / (maxY - minY);
            transX = -minX;
            transY = -minY;
        }
    }

    @Override
    public PointSource getSourceFor(PointSource eventSource) {
        if (eventSource instanceof MappedPoint) {
            return pointsBack.get((MappedPoint) eventSource);
        }
        return null;
    }

    @Override
    public List<PointSource> getSourceFor(List<PointSource> eventSource) {
        List<PointSource> out = new ArrayList<>();
        for (PointSource in : eventSource) {
            PointSource outP = getSourceFor(in);
            if (outP != null) {
                out.add(outP);
            }
        }
        return out;
    }

    @Override
    public PointPanel getPointPanel() {
        return pointPanel;
    }

    @Override
    public void setPoints(Collection<PointSource> list) {
        // reset gui
        pointPanel.setPoints(Collections.EMPTY_LIST);
        // reset model
        points.clear();
        pointsBack.clear();
        highlight.clear();

        // add all points
        for (PointSource p3 : list) {
            points.put(p3, null);
        }
        mapPoints();
    }

    private void mapPoints() {
        // map
        Set<PointSource> list = points.keySet();
        for (PointSource p3 : list) {
            MappedPoint mp = map3Dto2D(p3);
            points.put(p3, mp);
            pointsBack.put(mp, p3);
            log.fine(mp.getLocation().toString());
        }
        pointPanel.setPoints(points.values());
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (formerDimension != null && !getSize().equals(formerDimension)) {
            getSize(formerDimension);
            mapPoints();
        }
        super.paintComponent(g);
    }

    /**
     * Sets a highlighted point if it is contained in the panel
     * @param high
     */
    @Override
    public void setHighlight(Collection<PointSource> high) {
        highlight.clear();
        for (PointSource in : high) {
            if (in instanceof MappedPoint) {
                highlight.put(in, (MappedPoint) in);
            } else {
                highlight.put(in, points.get(in));
            }
        }
        pointPanel.setHighlight(highlight.values());
    }

    @Override
    public void setAttributNames(List<String> names) {
        if (names.size() < 3) {
            return;
        }
        leftLabel.setText(names.get(0));
        rightLabel.setText(names.get(1));
        topLabel.setText(names.get(2));
    }

    private double[] map3Dto2D(double[] coordsOld) {
        double[] coords = coordsOld.clone();
        // don't forget to set the scale and translate parameters first!!!!
        final double length = Vectors.length(coords);
        final double factor1 = Arrays2.sum(coords) * Math.sqrt(3);

        Arrays2.div(coords, length);
        double factor2 = length / factor1;
        Arrays2.mul(coords, factor2);
        Arrays2.add(coords, -1d / Math.sqrt(3));

        double newX = -1d / Math.sqrt(2) * coords[0] + 1d / Math.sqrt(2) * coords[1];
        double newY = -1d / Math.sqrt(6) * coords[0] - 1d / Math.sqrt(6) * coords[1] + 2d / Math.sqrt(6) * coords[2];

        // translate and scale into the space 0-1
        newX = (newX + transX) * scaleX;
        newY = (newY + transY) * scaleY;
        return new double[]{newX, newY};
    }

    private MappedPoint map3Dto2D(PointSource p3) {
        double[] new2d = map3Dto2D(p3.getCoordinates());
        assert Math2.isIn(0, new2d[0], 1) : "x out of bounds : " + new2d[0] + " || " + p3;
        assert Math2.isIn(0, new2d[1], 1) : "y out of bounds : " + new2d[1] + " || " + p3;
        return new MappedPoint(new2d, p3);
    }

//    private MappedPoint map3Dto2D_using_weighting(PointSource p3) {
//        double[] weights = weightToCoords(p3.getCoordinates());
//        double[] compare = new double[weights.length]; // d_a, d_b,d_c
//        double minD = Double.MAX_VALUE;
//        double minX = Double.NaN;
//        double minY = Double.NaN;
//        // not very efficient - but the circle equations can get QUITE ugly!
//        for (double x = 0; x <= 1; x += 1d / 200) {
//            double dxa = (x - a_x) * (x - a_x);
//            double dxb = (x - b_x) * (x - b_x);
//            double dxc = (x - c_x) * (x - c_x);
//            for (double y = c_y - 1; y <= 1; y += 1d / 200) {
//                compare[0] = Math.sqrt(dxa + (y - a_y) * (y - a_y)); // d_a
//                compare[1] = Math.sqrt(dxb + (y - b_y) * (y - b_y)); // d_b
//                compare[2] = Math.sqrt(dxc + (y - c_y) * (y - c_y)); // d_c
//                double dst = distance.dist(weights, compare);
//                if (dst < minD) {
//                    minD = dst;
//                    minX = x;
//                    minY = y;
//                }
//            }
//        }
//        return new MappedPoint(minX, minY, p3);
//    }
//    private double[] weightToCoords(double[] in) {
//        double[] out = in.clone();
//        double sum = Arrays2.sum(out); // ensure, that the array summs up to 1
//        Arrays2.div(out, sum);
//        for (int i = 0; i < out.length; i++) {
//            assert !Double.isNaN(out[i]) : "NaN at index " + i;
//            out[i] = 1 - out[i];
//        }
//        return out;
//    }
    public Collection<PointSource> getHighlighted() {
        return highlight.keySet();
    }

//    public Color getCoDomainBackground() {
//        return coDomainBackground;
//    }
//
//    public void setCoDomainBackground(Color coDomainBackground) {
//        this.coDomainBackground = coDomainBackground;
//    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pointPanel = new de.lmu.ifi.dbs.trafficmining.simplex.PointPanel();
        topLabel = new javax.swing.JLabel();
        leftLabel = new javax.swing.JLabel();
        rightLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        pointPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pointPanel.setDoubleBuffered(false);

        javax.swing.GroupLayout pointPanelLayout = new javax.swing.GroupLayout(pointPanel);
        pointPanel.setLayout(pointPanelLayout);
        pointPanelLayout.setHorizontalGroup(
            pointPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 431, Short.MAX_VALUE)
        );
        pointPanelLayout.setVerticalGroup(
            pointPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 322, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(pointPanel, gridBagConstraints);

        topLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        topLabel.setText("attribute 1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(topLabel, gridBagConstraints);

        leftLabel.setText("attribute 2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(leftLabel, gridBagConstraints);

        rightLabel.setText("attribute 3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(rightLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel leftLabel;
    private de.lmu.ifi.dbs.trafficmining.simplex.PointPanel pointPanel;
    private javax.swing.JLabel rightLabel;
    private javax.swing.JLabel topLabel;
    // End of variables declaration//GEN-END:variables

    class Simplex3DBackgroundpainter implements Painter {

        // try to cache the polygon because mapping the points from 3d to 2d
        // takes too much time if it's done on EVERY repaint
        private Polygon polygon = null;
        private Dimension dim = new Dimension(-1, -1);
        private BufferedImage img = null;
        private int bgAlpha = 100;

        @Override
        public void paint(Graphics2D g, Object object, int width, int height) {
            if (img == null || polygon == null || !dim.equals(new Dimension(width, height))) {
                polygon = new Polygon();
                { // build polygon
                    int x, y;
                    final double steps = .1;
                    for (double i = 0; i < 1; i += steps) {
                        Point2D coords = map3Dto2D(new P3(0, i, 1 - i)).getLocation();
                        x = (int) (coords.getX() * width);
                        y = height - (int) (coords.getY() * height);
                        polygon.addPoint(x, y);
                    }
                    for (double i = 0; i < 1; i += steps) {
                        Point2D coords = map3Dto2D(new P3(i, 1 - i, 0)).getLocation();
                        x = (int) (coords.getX() * width);
                        y = height - (int) (coords.getY() * height);
                        polygon.addPoint(x, y);
                    }
                    for (double i = 0; i < 1; i += steps) {
                        Point2D coords = map3Dto2D(new P3(1 - i, 0, i)).getLocation();
                        x = (int) (coords.getX() * width);
                        y = height - (int) (coords.getY() * height);
                        polygon.addPoint(x, y);
                    }
                }
                { // build image
                    final int w = 64;
                    img = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
                    Point a = new Point(0, w);
                    Point b = new Point(w, w);
                    Point c = new Point(w / 2, 0);
                    int R, G, B;
                    final int max = w;
                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < w; y++) {
                            R = (int) (255 - Math.max(0, Math.min(1, a.distance(x, y) / max)) * 255);
                            G = (int) (255 - Math.max(0, Math.min(1, b.distance(x, y) / max)) * 255);
                            B = (int) (255 - Math.max(0, Math.min(1, c.distance(x, y) / max)) * 255);
                            img.setRGB(x, y, new Color(R, G, B, bgAlpha).getRGB());
                        }
                    }
                }
            }
            dim = new Dimension(width, height);

            // g.setColor(getCoDomainBackground());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setClip(polygon);
            g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        }
    }

    class MappedPoint implements PointSource {

        private final Point2D p;
        private final PointSource s3;

        private MappedPoint(double x, double y, PointSource s3) {
            this.p = new Point2D.Double(x, y);
            this.s3 = s3;
        }

        private MappedPoint(double[] xy, PointSource s3) {
            this.p = new Point2D.Double(xy[0], xy[1]);
            this.s3 = s3;
        }

        @Override
        public Point2D getLocation() {
            return p;
        }

        @Override
        public String toString() {
            return s3.toString();
        }

        @Override
        public double[] getCoordinates() {
            return s3.getCoordinates();
        }
    }

    class P3 implements PointSource {

        private final double[] coords;

        public P3(double... coords) {
            this.coords = coords;
        }

        @Override
        public Point2D getLocation() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double[] getCoordinates() {
            return coords;
        }
    }
}
