/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.painter;

import de.lmu.ifi.dbs.trafficmining.utils.MapBounds;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.Painter;

/**
 *
 * @author Franz
 */
public class MapBoundsPainter implements Painter<JXMapViewer> {

    private final MapBounds bounds;
    private final Color area;
    private final Color border;

    public MapBoundsPainter(MapBounds bounds, Color area, Color border) {
        this.bounds = bounds;
        this.area = area;
        this.border = border;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int i, int i1) {
        Point2D topLeft = map.getTileFactory().geoToPixel(new GeoPosition(bounds.top, bounds.left), map.getZoom());
        Point2D bottomRight = map.getTileFactory().geoToPixel(new GeoPosition(bounds.bottom, bounds.right), map.getZoom());

        int x = (int) topLeft.getX();
        int y = (int) topLeft.getY();
        int w = (int) Math.abs(bottomRight.getX() - topLeft.getX());
        int h = (int) Math.abs(bottomRight.getY() - topLeft.getY());

        Rectangle r = new Rectangle(x, y, w, h);

        Rectangle viewPort = map.getViewportBounds();
        g.translate(-viewPort.x, -viewPort.y);
        if (area != null) {
            g.setColor(area);
            g.fill(r);
        }
        if (border != null) {
            g.setColor(border);
            g.draw(r);
        }
    }
}