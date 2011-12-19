package de.lmu.ifi.dbs.trafficmining.simplex;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

class SimplexHighlighter extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
        SimplexControl control = (SimplexControl) e.getSource();
        PointPanel src = control.getPointPanel();
        e = SwingUtilities.convertMouseEvent((Component) control, e, control.getPointPanel());
        src.setHighlight(src.getEpsilonPoints(e.getPoint(), src.getPointSize() / 2));
    }
}
