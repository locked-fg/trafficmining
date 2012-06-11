package de.lmu.ifi.dbs.trafficmining.painter;

import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
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

    private final int nodeSize = 2;
    private final int halfNodeSize = 1;
    private final int arrowSize = 5;
    private final double pi4 = Math.PI / 4;
    private final Color color = Color.blue;
    private List<Path<?, ? extends Node, ? extends Link>> paths;

    public void clear() {
        paths = Collections.EMPTY_LIST;
    }

    public void setPath(Path<?, ? extends Node, ? extends Link> path) {
        this.paths = new ArrayList<>(1);
        this.paths.add(path);
    }

    public void setPath(List<Path<?, ? extends Node, ? extends Link>> pathList) {
        this.paths = pathList;
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        Rectangle2D vp = OSMUtils.getViewport(map);
        int zoom = map.getZoom();

        g.setColor(color);
        TileFactory tf = map.getTileFactory();

        for (Path<?, ? extends Node, ? extends Link> path : paths) {
            paintPath(path, tf, zoom, vp, g);
        }
    }

    protected void paintPath(Path<?, ? extends Node, ? extends Link> path, TileFactory tf, int zoom, Rectangle2D vp, Graphics2D g) {
        Path last;
        List<Node> nodeList = new ArrayList<>();
        do {
            nodeList.clear();
            if (inViewPort(path, tf, zoom, vp)) {
                // paint last node (first one will be painted in next iteration)
                paintMainNode(path.getLast(), tf, zoom, vp, g);

                // paint link nodes
                Link link = path.getLink();
                if (link != null) {
                    nodeList.addAll(link.getNodes());
                    if (nodeList.isEmpty()) {
                        nodeList.addAll(listFromPath(path));
                    }
                } else {
                    nodeList.addAll(listFromPath(path));
                }
                if (!path.getLast().equals(nodeList.get(nodeList.size() - 1))) {
                    Collections.reverse(nodeList);
                }
                paintLink(nodeList, tf, zoom, vp, g);
            }

            last = path;
            path = path.getParent();
        } while (path != null);
        paintMainNode((Node) last.getFirst(), tf, zoom, vp, g);
    }

    private void paintLink(List<Node> nodes, TileFactory tf, int zoom, Rectangle2D vp, Graphics2D g) {
        Point2D src, dst;
        int x1, y1;
        int x2, y2;
        int dx, dy;

        src = tf.geoToPixel(nodes.get(0).getGeoPosition(), zoom);
        x1 = (int) (src.getX() - vp.getX());
        y1 = (int) (src.getY() - vp.getY());
        for (int j = 1; j < nodes.size(); j++) {
            dst = tf.geoToPixel(nodes.get(j).getGeoPosition(), zoom);

            // draw line between points
            x2 = (int) (dst.getX() - vp.getX());
            y2 = (int) (dst.getY() - vp.getY());
            g.drawLine(x1, y1, x2, y2);

            // draw arrow at target point
            dx = x1 - x2;
            dy = y1 - y2;
            drawArrow(x2, y2, dx, dy, g);

            //
            x1 = x2;
            y1 = y2;
        }
    }

    /**
     * Draws a little arrow at (posX,posY) showing ina direction that comes from
     * (dx,dy)
     *
     * @param posX x-location to draw
     * @param posY y-location to draw
     * @param dx delta x to determine the angle
     * @param dy delta y to determine the angle
     * @param graphics graphics to draw on
     */
    private void drawArrow(int posX, int posY, int dx, int dy, Graphics2D graphics) {
        double theta = Math.atan2(dy, dx); // -pi;pi
        dx = (int) (arrowSize * Math.cos(theta + pi4));
        dy = (int) (arrowSize * Math.sin(theta + pi4));
        graphics.drawLine(posX, posY, posX + dx, posY + dy);

        dx = (int) (arrowSize * Math.cos(theta - pi4));
        dy = (int) (arrowSize * Math.sin(theta - pi4));
        graphics.drawLine(posX, posY, posX + dx, posY + dy);
    }

    /**
     * Paints the start and end nodes for a path
     *
     * @param node
     * @param tf
     * @param zoom
     * @param vp
     * @param g
     */
    private void paintMainNode(Node node, TileFactory tf, int zoom, Rectangle2D vp, Graphics2D g) {
        Point2D srcPoint = tf.geoToPixel(node.getGeoPosition(), zoom);
        int x = (int) (srcPoint.getX() - vp.getX());
        int y = (int) (srcPoint.getY() - vp.getY());
        g.drawOval(x - halfNodeSize, y - halfNodeSize, nodeSize, nodeSize);
    }

    /**
     * Test if at least one node of the path is within the viewport
     *
     * @param path
     * @param tf
     * @param zoom
     * @param vp
     * @return true if both are in the viewport, false otherwise
     */
    private boolean inViewPort(Path<?, ? extends Node, ? extends Link> path, TileFactory tf, int zoom, Rectangle2D vp) {
        Point2D srcPoint = tf.geoToPixel(path.getFirst().getGeoPosition(), zoom);
        if (vp.contains(srcPoint)) {
            return true;
        }
        Point2D dstPoint = tf.geoToPixel(path.getLast().getGeoPosition(), zoom);
        if (vp.contains(dstPoint)) {
            return true;
        }
        return false;
    }

    private List<Node> listFromPath(Path<?, ? extends Node, ? extends Link> path) {
        List<Node> nodeList = new ArrayList<>(2);
        nodeList.add(path.getLocalStart());
        nodeList.add(path.getLast());
        return nodeList;
    }
}
