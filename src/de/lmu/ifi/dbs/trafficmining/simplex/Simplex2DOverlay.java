package de.lmu.ifi.dbs.trafficmining.simplex;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JPanel;

/**
 * Panel that can be drawn upon a SimplexControl2D and that paints a red line
 * inidcating a weight setting.
 * 
 * @author graf
 */
public class Simplex2DOverlay extends JPanel {

    private final Color lineColor = Color.red;
    private double weight = 0.5;
    public static final String PROP_WEIGHT = "weight";

    public Simplex2DOverlay() {
        super();
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        double oldWeight = this.weight;
        this.weight = weight;
        propertyChangeSupport.firePropertyChange(PROP_WEIGHT, oldWeight, weight);
        repaint();
    }
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(lineColor);

        if (weight < 0.5) {
            int x = (int) (w * 2 * weight);
            g2.drawLine(0, h, x, 0);
        } else if (weight >= 0.5) {
            int y = (int) (h * (weight * 2 - 1));
            g2.drawLine(0, h, w, y);
        }
    }
}
