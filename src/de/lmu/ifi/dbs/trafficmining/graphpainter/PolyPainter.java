package de.lmu.ifi.dbs.trafficmining.graphpainter;

import de.lmu.ifi.dbs.trafficmining.PBFtoOSMFrame;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.Painter;

/**
 *
 * @author wombat
 */
public class PolyPainter extends MouseAdapter implements Painter<JXMapViewer> {

    private GeoPosition[] geos;
    private Color color_region = Color.RED;
    private Color color_border = Color.RED;
    private boolean fill = true;
    private boolean paint_choosable = false;
    private Rectangle r_start, r_end;
    private boolean repaint = false;

    public PolyPainter() {
    }

    public PolyPainter(GeoPosition[] geo, Color region, Color border, boolean paint_choosable, boolean fill) {
        geos = geo;
        color_region = region;
        color_border = border;
        this.fill = fill;
        this.paint_choosable = paint_choosable;
    }

    public void setColorRegion(Color c) {
        color_region = c;
    }

    public void setColorBorder(Color c) {
        color_border = c;
    }

    public void setGeoPosition(GeoPosition[] geo) {
        geos = geo;
    }

    public void setFill(boolean b) {
        this.fill = b;
    }

    public void setPaintChoosable(boolean b) {
        this.paint_choosable = b;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            JXMapViewer map = ((JXMapViewer) e.getSource());
            r_start = new Rectangle(e.getPoint());
//            geo_pressed = map.convertPointToGeoPosition(e.getPoint());
            map.setPanEnabled(false);
        } else {
            r_start = null;
            r_end = null;
//            geo_pressed = null;
//            geo_dragged = null;
        }
        repaint = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (r_start != null) {
//        if (geo_pressed != null) {
            JXMapViewer map = ((JXMapViewer) e.getSource());
            r_end = new Rectangle(e.getPoint());
//            geo_dragged = map.convertPointToGeoPosition(e.getPoint());
            repaint = true;
            map.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && r_end != null) {
//        if (e.getButton() == MouseEvent.BUTTON1 && geo_dragged != null) {
            JXMapViewer map = ((JXMapViewer) e.getSource());
            map.setPanEnabled(true);
            map.repaint();
        }
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        if (repaint) {
//            Point2D sele_start = map.convertGeoPositionToPoint(geo_pressed);
//            Point2D sele_end = map.convertGeoPositionToPoint(geo_dragged);
//            Rectangle2D r_sele_start = new Rectangle2D.Double(sele_start.getX(), sele_start.getY(), 1, 1);
//            Rectangle2D r_sele_end = new Rectangle2D.Double(sele_end.getX(), sele_end.getY(), 1, 1);
//            Rectangle2D r_union = r_sele_start.createUnion(r_sele_end);
//            GeoPosition geo_tl = map.convertPointToGeoPosition(new Point2D.Double(r_union.getX(), r_union.getY()));
//            GeoPosition geo_tr = map.convertPointToGeoPosition(new Point2D.Double(r_union.getX() + r_union.getWidth(), r_union.getY()));
//            GeoPosition geo_br = map.convertPointToGeoPosition(new Point2D.Double(r_union.getX() + r_union.getWidth(), r_union.getY() + r_union.getHeight()));
//            GeoPosition geo_bl = map.convertPointToGeoPosition(new Point2D.Double(r_union.getX(), r_union.getY() + r_union.getHeight()));

            Rectangle r_union = r_start.union(r_end);
            GeoPosition geo_tl = map.convertPointToGeoPosition(new Point(r_union.x, r_union.y));
            GeoPosition geo_tr = map.convertPointToGeoPosition(new Point(r_union.x + r_union.width, r_union.y));
            GeoPosition geo_br = map.convertPointToGeoPosition(new Point(r_union.x + r_union.width, r_union.y + r_union.height));
            GeoPosition geo_bl = map.convertPointToGeoPosition(new Point(r_union.x, r_union.y + r_union.height));
            geos = new GeoPosition[]{geo_tl, geo_tr, geo_br, geo_bl};
            PBFtoOSMFrame.setLongLat(
                    new double[]{
                        geo_tl.getLongitude(),
                        geo_tr.getLongitude(),
                        geo_tl.getLatitude(),
                        geo_br.getLatitude(),});
            repaint = false;
        }
        if (geos != null) {
            g = (Graphics2D) g.create();
            //convert from viewport to world bitmap
            Rectangle bounds = map.getViewportBounds();
            g.translate(-bounds.x, -bounds.y);

            //create a polygon
            Polygon poly = new Polygon();
            for (GeoPosition gp : geos) {
                //convert geo to world bitmap pixel
                Point2D pt = map.getTileFactory().geoToPixel(gp, map.getZoom());
                poly.addPoint((int) pt.getX(), (int) pt.getY());
            }

            //do the drawing
            if (paint_choosable) {
                if (fill) {
                    g.setColor(color_region);
                    g.fill(poly);
                } else {
                    g.setColor(color_border);
                    g.draw(poly);
                }
            } else {
                g.setColor(color_border);
                g.draw(poly);
                g.setColor(color_region);
                g.fill(poly);
            }
            g.dispose();
        }
    }
}
