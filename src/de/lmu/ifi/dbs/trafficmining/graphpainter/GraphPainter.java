package de.lmu.ifi.dbs.trafficmining.graphpainter;

import de.lmu.ifi.dbs.trafficmining.TrafficminingProperties;
import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * @author graf
 */
public class GraphPainter extends AbstractPainter<JXMapViewer> {

    private static final Logger log = Logger.getLogger(GraphPainter.class.getName());
    private static final String LINK_PAINT_ATTRIBUTE = "highway";
    private HashMap<Integer, List<String>> zoomToLinkWhitelist = new HashMap<>();
    private final Color color = Color.red;
    private final Color colorOneWay = new Color(0, 100, 0);
    private OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph;
    private WeakHashMap<OSMNode, Point2D> geo2pixel = new WeakHashMap<>(1000);
    private int lastZoom = -1;
    private final double pi4 = Math.PI / 4;
    private final int arrowSize = 5;

    public GraphPainter() {
        try {
            File zoomToLinkFile = new File(TrafficminingProperties.ZOOM_WHITELIST_FILE);
            Properties prop = new Properties();
            prop.load(new BufferedReader(new FileReader(zoomToLinkFile)));
            log.log(Level.FINE, "Using zoom config: {0} / Zoomlevels found: {1}",
                    new Object[]{zoomToLinkFile.getAbsolutePath(), prop.keySet().size()});

            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                Integer zoomlevel = Integer.parseInt((String) entry.getKey());
                String temp = (String) entry.getValue();
                String[] temp_ar = temp.trim().split(";");
                List<String> list = Arrays.asList(temp_ar);
                zoomToLinkWhitelist.put(zoomlevel, list);
                log.log(Level.FINE, "{0} -> {1}", new Object[]{zoomlevel, list});
            }
        } catch (IOException | NumberFormatException e) {
            log.info("A error occured due parsing zoom config, using no zoom config at all");
            zoomToLinkWhitelist.clear();
        }
    }

    public void setGraph(OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph) {
        this.graph = graph;
        this.geo2pixel.clear();
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (graph == null) {
            return;
        }

        int zoom = map.getZoom();
        if (lastZoom != zoom) {
            geo2pixel.clear();
            lastZoom = zoom;
        }

        // figure out which waypoints are within this map viewport so, get the bounds
        Rectangle2D vp2 = OSMUtils.getViewport(map);
        Rectangle viewportBounds = map.getViewportBounds();
        TileFactory tf = map.getTileFactory();

        // primitive cache for checking if a node has been painted in the neighbourhoud already
        // dividing by 2 is done in order to protect a little area around each pixel
        boolean[][] pixels = new boolean[1 + ((int) viewportBounds.getWidth()) >> 1][1 + ((int) viewportBounds.getHeight()) >> 1];

        List<OSMNode> nodes = new ArrayList<>(graph.getNodes().size() / 10);
        HashSet processedLinks = new HashSet(1000);
        Point2D point;
        for (OSMNode<OSMLink> node : graph.getNodes()) {
            point = toPixel(node, tf, zoom);
            if (vp2.contains(point)) {
                // paint links?
                boolean painted = false;
                for (OSMLink<OSMNode> link : node.getLinks()) {
                    if (processedLinks.add(link) && isPaintable(link, zoom)) {
                        painted = true;
                        g.setColor(link.isOneway() ? colorOneWay : color);
                        paintLink(nodes, link, g, tf, zoom, vp2);
                    }
                }

                // only paint the nodes if at least one link has been painted above
                if (painted) {
                    // paint node as little dot IF the neighbourhood is unpainted
                    int x = (int) (point.getX() - vp2.getX());
                    int y = (int) (point.getY() - vp2.getY());
                    if (!pixels[x >> 1][y >> 1]) {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                        g.drawRect(x - 1, y - 1, 2, 2);
                        pixels[x >> 1][y >> 1] = true;
                    }
                }
            }
        }
    }

    /**
     * Check if this link should be painted on the according zoom level.
     *
     * @param link
     * @param zoom
     * @return true if it should be painted, false otherwise
     */
    private boolean isPaintable(OSMLink<OSMNode> link, Integer zoom) {
        String highway = link.getAttr(LINK_PAINT_ATTRIBUTE);

        // don't paint the link if it is not even a highway
        if (highway == null) {
            return false;
        }

        // just paint if no whitelist is defined
        if (zoomToLinkWhitelist.isEmpty()) {
            return true;
        }

        // invalid zoomlevel or end of recursion
        if (zoom <= 0) {
            return false;
        }

        // now check the whitelist
        List<String> whitelist = zoomToLinkWhitelist.get(zoom);
        if (whitelist == null) { // no list for this zoomlevel, try previous level
            return isPaintable(link, zoom - 1);
        }

        return whitelist.contains(highway);
    }

    private void paintLink(List<OSMNode> nodes, OSMLink<OSMNode> link, Graphics2D g, TileFactory tf, int zoom, Rectangle2D vp2) {
        int pixDist = length(link, tf, zoom);
        //System.out.println("pixDist: "+pixDist+"link: "+link+" zoom: "+zoom);
        if (pixDist <= 3) {
            return;
        }
        nodes.clear();
        if (pixDist >= 20) {
            nodes.addAll(link.getNodes());
        } else {
            nodes.add(link.getSource());
            nodes.add(link.getTarget());
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Point2D dst;
        Point2D src = toPixel(nodes.get(0), tf, zoom);
        int x1, y1, x2, y2;
        double delta;
        for (int i = 1; i < nodes.size(); i++) {
            dst = toPixel(nodes.get(i), tf, zoom);
            delta = src.distance(dst);
            // only paint links > 10px or links to the end 
            if (delta <= 10 && i < nodes.size() - 1) {
                continue;
            }
            x1 = (int) (src.getX() - vp2.getX());
            y1 = (int) (src.getY() - vp2.getY());
            x2 = (int) (dst.getX() - vp2.getX());
            y2 = (int) (dst.getY() - vp2.getY());
            g.drawLine(x1, y1, x2, y2);
            // paint arrows only on lines with a length of
            // 2*arrowsize. Bitshift again to be a bit faster
            if (link.isOneway() && delta > (arrowSize << 1)) {
                paintOneWay(x1, x2, y1, y2, g);
            }
            src = dst;
        }
    }

    // paint arrow
    private void paintOneWay(int x1, int x2, int y1, int y2, Graphics2D g) {
        // angle of the link
        double theta = Math.atan2(y1 - y2, x1 - x2); // -pi;pi
        int dx = (int) (arrowSize * Math.cos(theta + pi4));
        int dy = (int) (arrowSize * Math.sin(theta + pi4));

        // draw arrows between 2 nodes
        int sx = (x1 + x2) >> 1; // bit shift is faster than /2
        int sy = (y1 + y2) >> 1;
        g.drawLine(sx, sy, sx + dx, sy + dy);
        dx = (int) (arrowSize * Math.cos(theta - pi4));
        dy = (int) (arrowSize * Math.sin(theta - pi4));
        g.drawLine(sx, sy, sx + dx, sy + dy);
    }

    private Point2D toPixel(OSMNode n, TileFactory tf, int zoom) {
        Point2D p = geo2pixel.get(n);
        if (p == null) {
            p = tf.geoToPixel(n.getGeoPosition(), zoom);
            geo2pixel.put(n, p);
        }
        return p;
    }

    private int length(OSMLink<OSMNode> link, TileFactory tf, int zoom) {
        Point2D a = toPixel(link.getSource(), tf, zoom);
        Point2D b = toPixel(link.getTarget(), tf, zoom);
        return (int) a.distance(b);
    }
}
