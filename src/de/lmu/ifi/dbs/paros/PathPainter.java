package de.lmu.ifi.dbs.paros;

import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.utils.OSMUtils;
import de.lmu.ifi.dbs.paros.graph.Path;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 *
 * @author graf
 */
public class PathPainter extends AbstractPainter<JXMapViewer> {

    private final Color color = Color.blue;
    private List<Path<?, ? extends OSMNode, ?>> paths;

    public void clear(){
        paths = Collections.EMPTY_LIST;
    }

    public void setPath(Path<?, ? extends OSMNode, ?> path) {
        this.paths = new ArrayList<Path<?, ? extends OSMNode, ?>>(1);
        this.paths.add(path);
    }

    public void setPath(List<Path<?, ? extends OSMNode, ?>> pathList) {
        this.paths = pathList;
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (paths == null || paths.size() == 0) {
            return;
        }
        Rectangle2D vp = OSMUtils.getViewport(map);
        int zoom = map.getZoom();

        g.setColor(color);
        TileFactory tf = map.getTileFactory();

        for (Path<?, ? extends OSMNode, ?> path : paths) {
            paintPath(path, tf, zoom, vp, g);
        }
    }

    protected void paintPath(Path<?, ? extends OSMNode, ?> path, TileFactory tf, int zoom, Rectangle2D vp, Graphics2D g) {
        List<? extends OSMNode> list = path.getNodes();
        for (int i = 0; i < list.size() - 1; i++) {
            // paint link from 1st to 2nd node
            OSMNode srcNode = list.get(i);
            OSMNode dstNode = list.get(i + 1);
            // one of them in viewport?
            Point2D srcPoint = tf.geoToPixel(srcNode.getGeoPosition(), zoom);
            Point2D dstPoint = tf.geoToPixel(dstNode.getGeoPosition(), zoom);
            if (!vp.contains(srcPoint) && !vp.contains(dstPoint)) { // none in viewport
                continue;
            }
            {
                // paint nodes as little dots
                int x = (int) (srcPoint.getX() - vp.getX());
                int y = (int) (srcPoint.getY() - vp.getY());
                g.drawOval(x - 1, y - 1, 2, 2);
                x = (int) (dstPoint.getX() - vp.getX());
                y = (int) (dstPoint.getY() - vp.getY());
                g.drawOval(x - 1, y - 1, 2, 2);
            }

            // get ordered link between the nodes
            List<OSMNode> nodes = OSMUtils.orderedNodesBetween(srcNode, dstNode);
            
            // paint
            Point2D src, dst;
            int x1, y1;
            int x2, y2;
            int dx, dy;
            double pi4 = Math.PI / 4;
            src = tf.geoToPixel(nodes.get(0).getGeoPosition(), zoom);
            for (int j = 1; j < nodes.size(); j++) {
                dst = tf.geoToPixel(nodes.get(j).getGeoPosition(), zoom);
                // draw line between points
                x1 = (int) (src.getX() - vp.getX());
                y1 = (int) (src.getY() - vp.getY());
                x2 = (int) (dst.getX() - vp.getX());
                y2 = (int) (dst.getY() - vp.getY());
                g.drawLine(x1, y1, x2, y2);
                
                // draw arrow at target point
                dx = x1 - x2;
                dy = y1 - y2;
                double theta = Math.atan2(dy, dx); // -pi;pi
                dx = (int) (5 * Math.cos(theta + pi4));
                dy = (int) (5 * Math.sin(theta + pi4));
                g.drawLine(x2, y2, x2 + dx, y2 + dy);
                dx = (int) (5 * Math.cos(theta - pi4));
                dy = (int) (5 * Math.sin(theta - pi4));
                g.drawLine(x2, y2, x2 + dx, y2 + dy);
                src = dst;
            }
        }
//        List<? extends OSMNode> list = path.getNodes();
//        for (int i = 0; i < list.size() - 1; i++) {
//            // paint link from 1st to 2nd node
//            OSMNode srcNode = list.get(i);
//            OSMNode dstNode = list.get(i + 1);
//            // one of them in viewport?
//            Point2D srcPoint = tf.geoToPixel(srcNode.getGeoPosition(), zoom);
//            Point2D dstPoint = tf.geoToPixel(dstNode.getGeoPosition(), zoom);
//            if (!vp.contains(srcPoint) && !vp.contains(dstPoint)) { // none in viewport
//                continue;
//            }
//            {
//                // paint nodes as little dots
//                int x = (int) (srcPoint.getX() - vp.getX());
//                int y = (int) (srcPoint.getY() - vp.getY());
//                g.drawOval(x - 1, y - 1, 2, 2);
//                x = (int) (dstPoint.getX() - vp.getX());
//                y = (int) (dstPoint.getY() - vp.getY());
//                g.drawOval(x - 1, y - 1, 2, 2);
//            }
//
//            // get ordered link between the nodes
//            List<OSMNode> nodes = OSMUtils.orderedNodesBetween(srcNode, dstNode);
//            
//            // paint
//            Point2D src, dst;
//            int x1, y1;
//            int x2, y2;
//            int dx, dy;
//            double pi4 = Math.PI / 4;
//            src = tf.geoToPixel(nodes.get(0).getGeoPosition(), zoom);
//            for (int j = 1; j < nodes.size(); j++) {
//                dst = tf.geoToPixel(nodes.get(j).getGeoPosition(), zoom);
//                // draw line between points
//                x1 = (int) (src.getX() - vp.getX());
//                y1 = (int) (src.getY() - vp.getY());
//                x2 = (int) (dst.getX() - vp.getX());
//                y2 = (int) (dst.getY() - vp.getY());
//                g.drawLine(x1, y1, x2, y2);
//                
//                // draw arrow at target point
//                dx = x1 - x2;
//                dy = y1 - y2;
//                double theta = Math.atan2(dy, dx); // -pi;pi
//                dx = (int) (5 * Math.cos(theta + pi4));
//                dy = (int) (5 * Math.sin(theta + pi4));
//                g.drawLine(x2, y2, x2 + dx, y2 + dy);
//                dx = (int) (5 * Math.cos(theta - pi4));
//                dy = (int) (5 * Math.sin(theta - pi4));
//                g.drawLine(x2, y2, x2 + dx, y2 + dy);
//                src = dst;
//            }
//        }
    }
}

