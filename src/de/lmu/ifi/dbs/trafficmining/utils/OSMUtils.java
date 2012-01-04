package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.logging.Logger;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class OSMUtils {

    private static final Logger log = Logger.getLogger(OSMUtils.class.getName());

    // TODO provide a nice toString or s.th.
    public static enum PATH_ATTRIBUTES {

        /** nodes with degreee > 2 */
        NODES_DEG_GT2,
        /** Traffic signals highway=traffic_signals */
        TRAFFIC_SIGNALS
    }

    public static OSMNode getNearestNode(GeoPosition pos, OSMGraph g) {
        Collection<OSMNode> nodes = g.getNodes();

        double minDist = Double.MAX_VALUE;
        OSMNode nearest = null;

        for (OSMNode node : nodes) {
            double dist = dist(pos.getLatitude(), pos.getLongitude(), node);
            if (dist < minDist) {
                minDist = dist;
                nearest = node;
            }
        }

        return nearest;
    }

    /**
     * use only for nearest neightbour tests - does NOT reflect real distance
     * @param lat
     * @param lon
     * @param n
     * @return
     */
    private static double dist(double lat, double lon, OSMNode n) {
        double y = lat - n.getLat();
        double x = lon - n.getLon();
        return Math.sqrt(x * x + y * y);
    }

    public static double dist(OSMLink<OSMNode> link) {
        double dist = 0;
        List<OSMNode> nodes = link.getNodes();
        for (int i = 0; i < nodes.size() - 1; i++) {
            OSMNode a = nodes.get(i);
            OSMNode b = nodes.get(i + 1);
            dist += dist(a, b);
        }
        return dist;
    }

    /**
     *
     * @param a
     * @param b
     * @return distance in km
     */
    public static double dist(OSMNode a, OSMNode b) {
        double lat1 = a.getLat();
        double lon1 = a.getLon();
        double lat2 = b.getLat();
        double lon2 = b.getLon();

        double r = 6371.009; // earth radius
        double lat = (lat1 - lat2) * 3.1416 / 180;
        double lon = (lon1 - lon2) * 3.1416 / 180;
        double latm = (lat1 + lat2) / 2;
        double d = r * Math.sqrt(Math.pow(lat, 2) + Math.pow(lon * Math.cos(latm), 2));

        // now integrate elevation
        double heightDiff = a.getHeight() - b.getHeight();
        heightDiff /= 1000; // m -> km
        d = Math.sqrt(d * d + heightDiff * heightDiff);

        return d;
    }

    public static List<OSMLink<OSMNode>> split(final OSMLink<OSMNode> l, final OSMNode n) {
//        if (log.isLoggable(Level.FINE)) {
//            log.fine("splitting link " + l + ", node: " + n);
//        }

        List<OSMNode> nodes = getOrderedNodes(l);
        int index = nodes.indexOf(n);
        if (index < 0) {
            throw new IllegalArgumentException("node not in list");
        }

        // create 2 sublists
        List<OSMNode> newListA = nodes.subList(0, index + 1);
        List<OSMNode> newListB = nodes.subList(index, nodes.size());
        assert newListA.get(newListA.size() - 1).equals(newListB.get(0));

        // unbind the link from it's source/target
        l.getSource().removeLink(l);
        l.getTarget().removeLink(l);

        // create 2 new Links from the old one
        List<OSMLink<OSMNode>> result = new ArrayList<OSMLink<OSMNode>>(2);
        result.add(listToLink(newListA, l));
        result.add(listToLink(newListB, l));
        return result;
    }

    public static OSMLink<OSMNode> listToLink(List<OSMNode> list, OSMLink<OSMNode> l) {
        assert list.size() > 1 : "list size 1?";
        Map<String, String> attributes = l.getAttr();
        OSMLink<OSMNode> link = new OSMLink<OSMNode>(list.get(0), list.get(list.size() - 1), l.isOneway());
        link.setId(l.getId());
        double dist = 0;
        double asc = 0;
        double dsc = 0;

        for (int i = 0; i < list.size() - 1; i++) {
            OSMNode a = list.get(i);
            OSMNode b = list.get(i + 1);

            dist += dist(a, b);
            double diff = b.getHeight() - a.getHeight();
            if (diff > 0) {
                asc += diff;
            } else if (diff < 0) {
                dsc -= diff;
            }
        }

        link.setAscend(asc);
        link.setDescend(dsc);
        link.setDistance(dist);
        link.addNodes(list);
        link.setSpeed(l.getSpeed());
        // copy attributes
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            link.setAttr(entry.getKey(), entry.getValue());
        }

        return link;
    }

    public static List<OSMNode> getOrderedNodes(OSMLink<OSMNode> link) {
        List<OSMNode> nodes = new ArrayList<OSMNode>();
        nodes.addAll(link.getNodes());
        if (nodes.size() == 0) {
            nodes.add(link.getSource());
            nodes.add(link.getTarget());
            return nodes;
        }

        if (!nodes.get(0).equals(link.getSource())) {
            Collections.reverse(nodes);
        }
        return nodes;
    }

    public static List<OSMNode> orderedNodesBetween(OSMNode srcNode, OSMNode dstNode) {
        List<OSMNode> nodes = new ArrayList<OSMNode>();
        OSMLink<OSMNode> link = srcNode.getLinkTo(dstNode);
        if (link == null) {
            nodes.add(srcNode);
            nodes.add(dstNode);
            return nodes;
        }

        nodes.addAll(link.getNodes());
        if (nodes.size() == 0) {
            nodes.add(srcNode);
            nodes.add(dstNode);
            return nodes;
        }

        if (!nodes.get(0).equals(srcNode)) {
            Collections.reverse(nodes);
        }
        return nodes;
    }

    public static Rectangle2D getViewport(JXMapViewer map) {
        //figure out which waypoints are within this map viewport
        //so, get the bounds
        int zoom = map.getZoom();
        Rectangle viewportBounds = map.getViewportBounds();
        Dimension sizeInTiles = map.getTileFactory().getMapSize(zoom);
        int tileSize = map.getTileFactory().getTileSize(zoom);
        Dimension sizeInPixels = new Dimension(sizeInTiles.width * tileSize, sizeInTiles.height * tileSize);

        double vpx = viewportBounds.getX();
        // normalize the left edge of the viewport to be positive
        while (vpx < 0) {
            vpx += sizeInPixels.getWidth();
        }
        // normalize the left edge of the viewport to no wrap around the world
        while (vpx > sizeInPixels.getWidth()) {
            vpx -= sizeInPixels.getWidth();
        }

        // create two new viewports next to eachother
        return new Rectangle2D.Double(vpx,
                viewportBounds.getY(), viewportBounds.getWidth(), viewportBounds.getHeight());
    }

    public static Map<PATH_ATTRIBUTES, String> getPathInfos(List<OSMNode<OSMLink>> nodes) {
//        int crossings = 0; // http://wiki.openstreetmap.org/wiki/Key:crossing
        int nodecount = 0; // nodes with degree > 2
        int trafficsignals = 0; // traffic signals
        OSMNode<OSMLink> start = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            String highway = start.getAttr("highway");
            if (highway != null && highway.equals("traffic_signals")) {
                trafficsignals++;
            }
            nodecount += start.getLinks().size() > 2 ? 1 : 0;
            start = nodes.get(i);
        }

        HashMap<PATH_ATTRIBUTES, String> map = new HashMap<PATH_ATTRIBUTES, String>();
        map.put(PATH_ATTRIBUTES.NODES_DEG_GT2, Integer.toString(nodecount));
        map.put(PATH_ATTRIBUTES.TRAFFIC_SIGNALS, Integer.toString(trafficsignals));
        return map;
    }

    /**
     * Rough check if a and b are overlapping in 1 endpoint.
     * True is returned, if a and b share ONE of their endpoints and no other
     * endpoint is contained in the opponents path. So s.th like
     * A1--B1--A2B2 will not be connectable
     *
     * @param a
     * @param b
     * @return true if a and b might be connectable, false if they have the same start/endpoints or overlap
     */
    public static boolean isConnectable(Path a, Path b) {
        Node a1 = a.getFirst();
        Node a2 = a.getLast();
        Node b1 = b.getFirst();
        Node b2 = b.getLast();

        boolean b1InA = a.contains(b1);
        boolean b2InA = a.contains(b2);
        boolean a1InB = b.contains(a1);
        boolean a2InB = b.contains(a2);

        // a2-a1 b1-b2
        if (a1.equals(b1) && !b2InA && !a2InB) {
            return true;
        }
        // a2-a1 b2-b1
        if (a1.equals(b2) && !b1InA && !a2InB) {
            return true;
        }
        // a1-a2 b1-b2
        if (a2.equals(b1) && !b2InA && !a1InB) {
            return true;
        }
        // a1-a2 b2-b1
        if (a2.equals(b2) && !b1InA && !a1InB) {
            return true;
        }
        return false;
    }
    
        /**
     * http://wiki.openstreetmap.org/wiki/MaxSpeed_Overlay_Kosmos_Rules
     * http://wiki.openstreetmap.org/wiki/DE:MaxSpeed_Karte
     * @FIXME move this method into a utility class
     * @param l
     */
    public static void setSpeed(OSMGraph g, OSMLink l) {
        Map<String, Integer> speed = g.getSpeedMap();
        
        assert speed != null : "speed object is null?";
        assert l != null : "link is null?";

        String maxSpeedValue = l.getAttr("maxspeed");
        if (maxSpeedValue != null) { // maxSpeed set. try to use it
            if (maxSpeedValue.contains(";")) {
                log.fine("Link [" + l + "]: multiple values in maxspeed. Using first of: " + maxSpeedValue);
                maxSpeedValue = maxSpeedValue.split(";")[0];
            }
            try {
                l.setSpeed(Integer.parseInt(maxSpeedValue));
            } catch (NumberFormatException i) {
                // Not a number :-/
                if (maxSpeedValue.equals("walk")) {
                    l.setSpeed(speed.get("footway"));
                } else if (maxSpeedValue.startsWith("footway")) {
                    l.setSpeed(speed.get("footway"));
                } else if (maxSpeedValue.equals("variable")) {
                    l.setSpeed(speed.get("footway"));
                } else {
                    log.fine("Link [" + l + "]: Unmapped maxspeed value: " + maxSpeedValue + ". Use highway type.");
                }
            }

            // okay we've been successfull with the maxspeed attribute
            if (l.getSpeed() > 0) {
                return;
            }
        }

        // no maxspeed set or it was unparseable. Try to map the highway types
        String highway = l.getAttr("highway");
        if (highway != null) {
            Integer maxSpeedInt = speed.get(highway);

            if (maxSpeedInt == null && highway.contains(";")) {
                log.fine("Link [" + l + "]: multiple highway settings. Using first of: " + highway);
                maxSpeedInt = speed.get(highway.split(";")[0]);
            }
            if (maxSpeedInt == null) {
                log.fine("Link [" + l + "]: unknown highway type: " + highway + ". Setting default.");
                maxSpeedInt = speed.get("default");
            }
            l.setSpeed(maxSpeedInt);
            return;

        }

        // neither maxspeed nor highway gave a hint about the speed of this link
        log.info("Link [" + l + "]: no highway type set, using default.");
        l.setSpeed(speed.get("default"));
    }
}
