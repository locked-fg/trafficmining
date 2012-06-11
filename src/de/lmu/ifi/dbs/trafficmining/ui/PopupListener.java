package de.lmu.ifi.dbs.trafficmining.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 *
 * @author Franz
 */
public class PopupListener extends MouseAdapter {

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            JPopupMenu popupMenu = ((JComponent) e.getComponent()).getComponentPopupMenu();
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}