package de.lmu.ifi.dbs.beansUI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import javax.swing.JColorChooser;

public class ColorPropertyEditor extends PropertyEditorSupport {

    Color paint = new Color(0, 128, 255);
    JColorChooser picker = new JColorChooser();

    /** Creates a new instance of Paint2PropertyEditor */
    public ColorPropertyEditor() {
        picker.addPropertyChangeListener("paint", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                paint = picker.getColor();
                firePropertyChange();
            }
        });

    }

    @Override
    public Color getValue() {
        return paint;
    }

    @Override
    public void setValue(Object object) {
        paint = (Color) object;
        picker.setColor(paint);
        super.setValue(object);
    }

    @Override
    public String getJavaInitializationString() {
        Color paint1 = getValue(); // ?
        return paint1 == null ? "null" : "class.awt.Color";
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        // java.awt.Color[r=255,g=51,b=0]
        // [255,51,0]
        text = text.replaceAll("[^0-9,]", "");
        String[] parts = text.split(",");
        int[] ints = new int[3];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = Integer.parseInt(parts[i]);
        }
        Color c = new Color(ints[0], ints[1], ints[2]);
        picker.setColor(c);
        paint = c;
    }

    @Override
    public String getAsText() {
        return String.format("[%d,%d,%d]", paint.getRed(), paint.getGreen(), paint.getBlue());
    }

    @Override
    public void paintValue(Graphics g, Rectangle box) {
        if (g == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(paint);
        g2.fill(box);
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        return picker;
    }
}
