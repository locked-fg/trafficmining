package de.lmu.ifi.dbs.trafficmining.painter;

import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.painter.AbstractPainter;

public class NodePainter extends AbstractPainter<JXMapViewer> {

    private final Color colorFill = new Color(0, 200, 0, 180);
    private final Color colorBorder = new Color(100, 100, 100, 100);
    private List<Node> nodes;
    private final int size = 8;

    public NodePainter() {
        setAntialiasing(true);
    }

    public void clear() {
        nodes = Collections.EMPTY_LIST;
    }

    public void setNodes(Collection<? extends Node> nodeList) {
        nodes = new ArrayList<>(nodeList);
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Rectangle2D vp = OSMUtils.getViewport(map);
        int zoom = map.getZoom();

        TileFactory tf = map.getTileFactory();

        int halfSize = size / 2;
        for (Node node : nodes) {
            Point2D srcPoint = tf.geoToPixel(node.getGeoPosition(), zoom);
            if (!vp.contains(srcPoint)) {
                continue;
            }
            // paint nodes as little dots
            int x = (int) (srcPoint.getX() - vp.getX());
            int y = (int) (srcPoint.getY() - vp.getY());
            g.setColor(colorFill);
            g.fillOval(x - halfSize, y - halfSize, size, size);
            g.setColor(colorBorder);
            g.drawOval(x - halfSize, y - halfSize, size, size);
        }
    }
}
