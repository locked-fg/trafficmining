package de.lmu.ifi.dbs.trafficmining.simplex;

import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jdesktop.swingx.JXPanel;

/**
 * @author graf
 */
public class SimplexControl2D extends JXPanel implements SimplexControl {

    /** Creates new form SimplexControl2D */
    public SimplexControl2D() {
        initComponents();
        List<String> names = new ArrayList<String>(2);
        names.add("x");
        names.add("y");
        setAttributNames(names);
    }

    @Override
    public PointPanel getPointPanel() {
        return pointPanel;
    }

    @Override
    public void setPoints(Collection<PointSource> ps) {
        pointPanel.setPoints(ps);
    }

    @Override
    public void setHighlight(Collection<PointSource> ps) {
        pointPanel.setHighlight(ps);
    }

    @Override
    public void setAttributNames(List<String> names) {
        if (names.size() < 2) {
            return;
        }
        xLabel.setText(names.get(0));
        yLabel.setText(names.get(1));
        leftAttributeLabel.setText(names.get(1));
        rightAttributeLabel.setText(names.get(0));
    }

    @Override
    public PointSource getSourceFor(PointSource eventSource) {
        if (pointPanel.getPoints().contains(eventSource)) {
            return eventSource;
        } else {
            return null;
        }
    }

    @Override
    public List<PointSource> getSourceFor(List<PointSource> eventSource) {
        List<PointSource> out = new ArrayList<PointSource>();
        for (PointSource in : eventSource) {
            PointSource p = getSourceFor(in);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        zeroLabel = new javax.swing.JLabel();
        xLabel = new javax.swing.JLabel();
        yLabel = new javax.swing.JLabel();
        leftAttributeLabel = new javax.swing.JLabel();
        rightAttributeLabel = new javax.swing.JLabel();
        attributeSlider = new javax.swing.JSlider();
        pointPanel = new de.lmu.ifi.dbs.trafficmining.simplex.PointPanel();
        simplex2DOverlay = new de.lmu.ifi.dbs.trafficmining.simplex.Simplex2DOverlay();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new java.awt.GridBagLayout());

        zeroLabel.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        add(zeroLabel, gridBagConstraints);

        xLabel.setText("max");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        add(xLabel, gridBagConstraints);

        yLabel.setText("max");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        add(yLabel, gridBagConstraints);

        leftAttributeLabel.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        add(leftAttributeLabel, gridBagConstraints);

        rightAttributeLabel.setText("Speed");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        add(rightAttributeLabel, gridBagConstraints);

        attributeSlider.setMajorTickSpacing(10);
        attributeSlider.setMinorTickSpacing(5);
        attributeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                attributeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(9, 1, 0, 1);
        add(attributeSlider, gridBagConstraints);
        simplex2DOverlay.setWeight(attributeSlider.getValue() / 100d);

        pointPanel.setLayout(new java.awt.BorderLayout());

        simplex2DOverlay.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        simplex2DOverlay.setOpaque(false);
        simplex2DOverlay.setLayout(new java.awt.BorderLayout());
        pointPanel.add(simplex2DOverlay, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(pointPanel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void attributeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_attributeSliderStateChanged
        simplex2DOverlay.setWeight(attributeSlider.getValue() / 100d);
    }//GEN-LAST:event_attributeSliderStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider attributeSlider;
    private javax.swing.JLabel leftAttributeLabel;
    private de.lmu.ifi.dbs.trafficmining.simplex.PointPanel pointPanel;
    private javax.swing.JLabel rightAttributeLabel;
    private de.lmu.ifi.dbs.trafficmining.simplex.Simplex2DOverlay simplex2DOverlay;
    private javax.swing.JLabel xLabel;
    private javax.swing.JLabel yLabel;
    private javax.swing.JLabel zeroLabel;
    // End of variables declaration//GEN-END:variables
}