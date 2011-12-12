package de.lmu.ifi.dbs.paros.simplex;

import de.lmu.ifi.dbs.paros.TrafficminingGUI;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;
import javax.swing.UIManager;

/**
 * @author graf/
 */
public class SimplexTest extends javax.swing.JFrame {

    public SimplexTest() {
        initComponents();
        { // 1D
            PointPanel pp1 = d1.getPointPanel();
            List<PointPanel.PointSource> list = new ArrayList<PointPanel.PointSource>();
            List<PointPanel.PointSource> high = new ArrayList<PointPanel.PointSource>();
            final PS pS = new PS(0.2, 0.0);
            list.add(pS);
            high.add(pS);
            list.add(new PS(0d, 0d));
            list.add(new PS(0.1, 0d));
            list.add(new PS(0.5, 0d));
            list.add(new PS(1d, 0d));
            pp1.setPoints(list);
            pp1.setHighlight(high);
            pp1.addMouseListener(new SimplexHighlighter());
        }
        { // 2D
            PointPanel pp2 = d2.getPointPanel();
            List<PointPanel.PointSource> list = new ArrayList<PointPanel.PointSource>();
            List<PointPanel.PointSource> high = new ArrayList<PointPanel.PointSource>();
            final PS pS = new PS(0.2, 0.1);
            list.add(pS);
            high.add(pS);
            list.add(new PS(0d, 0d));
            list.add(new PS(0.1, .1));
            list.add(new PS(0.5, .2));
            list.add(new PS(0.5, .4));
            list.add(new PS(1d, .5));
            pp2.setPoints(list);
            pp2.setHighlight(high);
            pp2.addMouseListener(new SimplexHighlighter());
        }
        { // 3D
            d3.setAttributNames(Arrays.asList(new String[]{"1", "2", "3"}));

            List<PointPanel.PointSource> list = new ArrayList<PointPanel.PointSource>();
            List<PointPanel.PointSource> high = new ArrayList<PointPanel.PointSource>();
//            list.add(new SimplexPoint3d(0, 0, 1));
//            list.add(new SimplexPoint3d(0, 1, 0));
            list.add(new SimplexPoint3d(1, 0, 0));

//            PointPanel.PointSource ps1 = new SimplexPoint3d(0., .0, 1.0);
//            high.add(ps1);
//            list.add(ps1);
//            PointPanel.PointSource ps2 = new SimplexPoint3d(2, 2, 2);
//            high.add(ps2);
//            list.add(ps2);

//            int t = 10;
//            for (double i = 0; i < 1; i += 1d / t) {
//                list.add(new SimplexPoint3d(i, 1 - i, 0));
//                list.add(new SimplexPoint3d(i, 0, 1 - i));
//                list.add(new SimplexPoint3d(0, i, 1 - i));
//            }
            d3.setPoints(list);
            d3.setHighlight(high);
            d3.addMouseListener(new SimplexHighlighter());
        }
    }

    public static class PS implements PointPanel.PointSource {

        final Point2D p;

        public PS(double x, double y) {
            this.p = new Point.Double(x, y);
        }

        @Override
        public Point2D getLocation() {
            return p;
        }

        @Override
        public String toString() {
            return String.format("%.3f, %.3f", p.getX(), p.getY());
        }

        @Override
        public double[] getCoordinates() {
            return new double[]{p.getX(), p.getY()};
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

        javax.swing.JTabbedPane jTabbedPane1 = new javax.swing.JTabbedPane();
        d1 = new de.lmu.ifi.dbs.paros.simplex.SimplexControl1D();
        d3 = new de.lmu.ifi.dbs.paros.simplex.SimplexControl3D();
        d2 = new de.lmu.ifi.dbs.paros.simplex.SimplexControl2D();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPane1.addTab("tab3", d1);
        jTabbedPane1.addTab("tab3", d3);
        jTabbedPane1.addTab("tab2", d2);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        try (InputStream is = SimplexTest.class.getResourceAsStream("/logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                is.close();
            }
        } catch (IOException ex) {
        }
        
        
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new SimplexTest().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private de.lmu.ifi.dbs.paros.simplex.SimplexControl1D d1;
    private de.lmu.ifi.dbs.paros.simplex.SimplexControl2D d2;
    private de.lmu.ifi.dbs.paros.simplex.SimplexControl3D d3;
    // End of variables declaration//GEN-END:variables
}
