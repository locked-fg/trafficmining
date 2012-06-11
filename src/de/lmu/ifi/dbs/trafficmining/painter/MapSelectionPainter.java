/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.painter;

import de.lmu.ifi.dbs.trafficmining.utils.MapBounds;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.Painter;

/**
 *
 * @author Franz
 */
public class MapSelectionPainter extends MouseAdapter implements Painter<JXMapViewer> {

    private Rectangle paintRectangle, start, end;
    private MapBounds bounds;
    private Color borderColor = new Color(0, 0, 200);
    private Color regionColor = new Color(0, 0, 200, 100);

    public MapSelectionPainter() {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            paintRectangle = null;
            start = null;
        } else {
            start = new Rectangle(e.getPoint());
            ((JXMapViewer) e.getSource()).setPanEnabled(false);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (start != null) {
            end = new Rectangle(e.getPoint());
            paintRectangle = start.union(end);
            updateBounds(((JXMapViewer) e.getSource()));
        }
        ((JXMapViewer) e.getSource()).repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (start == null) {
            return;
        }
        end = new Rectangle(e.getPoint());
        paintRectangle = start.union(end);
        updateBounds(((JXMapViewer) e.getSource()));

        ((JXMapViewer) e.getSource()).setPanEnabled(true);
        ((JXMapViewer) e.getSource()).repaint();
    }

    @Override
    public void paint(Graphics2D gd, JXMapViewer map, int i, int i1) {
        if (paintRectangle != null) {
            gd.setColor(regionColor);
            gd.fillRect(paintRectangle.x, paintRectangle.y, paintRectangle.width, paintRectangle.height);
            gd.setColor(borderColor);
            gd.drawRect(paintRectangle.x, paintRectangle.y, paintRectangle.width, paintRectangle.height);
        }
    }

    public MapBounds getBounds() {
        return bounds;
    }

    private void updateBounds(JXMapViewer map) {
        Point topLeft = new Point(paintRectangle.x, paintRectangle.y);
        Point bottomRight = new Point(paintRectangle.x + paintRectangle.width,
                paintRectangle.y + paintRectangle.height);

        Rectangle viewPort = map.getViewportBounds();
        topLeft.translate(viewPort.x, viewPort.y);
        bottomRight.translate(viewPort.x, viewPort.y);

        GeoPosition topLeftGeo = map.getTileFactory().pixelToGeo(topLeft, map.getZoom());
        GeoPosition bottomRightGeo = map.getTileFactory().pixelToGeo(bottomRight, map.getZoom());

        bounds = new MapBounds(topLeftGeo, bottomRightGeo);
    }
}
