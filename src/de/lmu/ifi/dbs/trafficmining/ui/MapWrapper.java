package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.TileServer;
import de.lmu.ifi.dbs.trafficmining.TileServerFactory;
import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.painter.GraphPainter;
import de.lmu.ifi.dbs.trafficmining.painter.NodePainter;
import de.lmu.ifi.dbs.trafficmining.painter.PathPainter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import org.jdesktop.swingx.JXMapKit.DefaultProviders;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;

/**
 * Wrapper class around the map component.
 *
 * This class is intended to manage waypoint painting and stuff. The aim is to
 * get somewhat more independent from the singX implementation. Currently all
 * method calls are delegated to the map component. Yet they are all marked as
 * deprecated in orer to provide some aid in getting rif of these methods.
 *
 * @author Franz
 */
public class MapWrapper extends javax.swing.JPanel {

    static final Logger log = Logger.getLogger(MapWrapper.class.getName());
    private final JXMapViewer map;
    private Map<String, TileServer> tileservers = new HashMap<>();
    private TileServer tileServer;
    //
    private HashMap<Object, WaypointPainter<JXMapViewer>> painters = new HashMap<>();
    private final GraphPainter graphPainter = new GraphPainter();
    private final PathPainter pathPainter = new PathPainter();
    private final NodePainter nodePainter = new NodePainter();

    /**
     * Creates new form MapWrapper
     */
    public MapWrapper() {
        initComponents();
        map = mapKit.getMainMap();
        initTileServers();
        map.setAddressLocation(new GeoPosition(47.75996, 11.5652));
        map.setZoom(3);
        map.repaint();
    }

    private void initTileServers() {
        try {
            TileServerFactory tileServerFactory = TileServerFactory.get();
            tileservers = tileServerFactory.getTileServers();
            setTileServer(tileServerFactory.getDefaultServer());

            // load more tiles in parallel
            // MIND THE TILE USE POLICY IF USING OSM DIRECTLY
            // http://wiki.openstreetmap.org/wiki/Tile_usage_policy
            // FIXME put this into a property
            ((DefaultTileFactory) map.getTileFactory()).setThreadPoolSize(3);
            map.setRestrictOutsidePanning(true);
            map.setHorizontalWrapped(false);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public void setNodes(Object key, Collection<Node> nodes) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        if (nodes == null) {
            throw new NullPointerException("nodes must not be null");
        }

        painters.remove(key);
        if (!nodes.isEmpty()) {
            WaypointPainter<JXMapViewer> wp = new WaypointPainter<>();
            wp.setWaypoints(toWaypoints(nodes));
            painters.put(key, wp);
        }
        updatePainters();
    }

    private Set<Waypoint> toWaypoints(Collection<Node> nodes) {
        HashSet<Waypoint> set = new HashSet<>();
        for (Node n : nodes) {
            set.add(new Waypoint(n.getLat(), n.getLon()));
        }
        return set;
    }

    /**
     * Transforms a xy-coordinate in Swing-coordinates (0,0 is the top left) to
     * latitude/longitude.
     *
     * @param p
     * @return
     */
    public GeoPosition xyToGeo(Point p) {
        Rectangle viewport = map.getViewportBounds();
        p.translate((int) viewport.getX(), (int) viewport.getY());

        GeoPosition pos = map.getTileFactory().pixelToGeo(p, map.getZoom());
        return pos;
    }

    public void setTileServer(String key) {
        if (tileservers.containsKey(key)) {
            setTileServer(tileservers.get(key));
        }
    }

    private void setTileServer(TileServer tileServer) {
        if (this.tileServer == tileServer) {
        }

        if (tileServer.getTileFactory() != null) {
            if (!tileServer.isValid()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Tileserver \"" + tileServer.getBaseURL() + "\" seems to be broken."
                        + "\nPlease re-check all properties.",
                        "Tileserver is broken.", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Tileserver \"" + tileServer.getBaseURL() + "\" has no own TileFactory set up."
                    + "\nPlease re-check your code and enable it.",
                    "Tileserver not initialized.", JOptionPane.ERROR_MESSAGE);
        }

        this.tileServer = tileServer;
        map.setTileFactory(tileServer.getTileFactory());
    }

    public Set<String> getTileServers() {
        return tileservers.keySet();
    }

    public String currentTileserverName() {
        for (Map.Entry<String, TileServer> entry : tileservers.entrySet()) {
            if (entry.getValue().equals(tileServer)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
        map.addMouseListener(l);
    }

    @Override
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        map.addMouseMotionListener(l);
    }

    @Override
    public synchronized void removeMouseListener(MouseListener l) {
        map.removeMouseListener(l);
    }

    private void updatePainters() {
        List<Painter> values = new ArrayList<>();
        values.add(graphPainter);
        values.add(pathPainter);
        values.add(nodePainter);
        values.addAll(painters.values());

        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>();
        compoundPainter.setPainters(values.toArray(new Painter[]{}));
        map.setOverlayPainter(compoundPainter);
        map.repaint();
    }

    public void paintGraph(Graph<Node<Link>, Link<Node>> graph) {
        graphPainter.setGraph(graph);
        updatePainters();
    }

    public void paintPaths(List<Path<?, ? extends Node, ? extends Link>> list) {
        pathPainter.setPath(list);
        updatePainters();
    }

    public void paintNodes(List<Node> l) {
        nodePainter.setNodes(l);
        updatePainters();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mapKit = new org.jdesktop.swingx.JXMapKit();

        setLayout(new java.awt.BorderLayout());

        mapKit.setMiniMapVisible(false);
        add(mapKit, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jdesktop.swingx.JXMapKit mapKit;
    // End of variables declaration//GEN-END:variables

    //<editor-fold defaultstate="collapsed" desc="delegates to mapKit instance">
    @Deprecated
    public void setOverlayPainter(Painter overlay) {
        map.setOverlayPainter(overlay);
    }

    @Deprecated
    public void calculateZoomFrom(Set<GeoPosition> positions) {
        map.calculateZoomFrom(positions);
    }

    @Deprecated
    public int getZoom() {
        return map.getZoom();
    }

    @Deprecated
    public void setZoom(int zoom) {
        mapKit.setZoom(zoom);
    }

    @Deprecated
    public Action getZoomOutAction() {
        return mapKit.getZoomOutAction();
    }

    @Deprecated
    public Action getZoomInAction() {
        return mapKit.getZoomInAction();
    }

    @Deprecated
    public boolean isMiniMapVisible() {
        return mapKit.isMiniMapVisible();
    }

    @Deprecated
    public void setMiniMapVisible(boolean miniMapVisible) {
        mapKit.setMiniMapVisible(miniMapVisible);
    }

    @Deprecated
    public boolean isZoomSliderVisible() {
        return mapKit.isZoomSliderVisible();
    }

    @Deprecated
    public void setZoomSliderVisible(boolean zoomSliderVisible) {
        mapKit.setZoomSliderVisible(zoomSliderVisible);
    }

    @Deprecated
    public boolean isZoomButtonsVisible() {
        return mapKit.isZoomButtonsVisible();
    }

    @Deprecated
    public void setZoomButtonsVisible(boolean zoomButtonsVisible) {
        mapKit.setZoomButtonsVisible(zoomButtonsVisible);
    }

    @Deprecated
    public void setTileFactory(TileFactory fact) {
        mapKit.setTileFactory(fact);
    }

    @Deprecated
    public void setCenterPosition(GeoPosition pos) {
        mapKit.setCenterPosition(pos);
    }

    @Deprecated
    public GeoPosition getCenterPosition() {
        return mapKit.getCenterPosition();
    }

    @Deprecated
    public GeoPosition getAddressLocation() {
        return mapKit.getAddressLocation();
    }

    @Deprecated
    public void setAddressLocation(GeoPosition pos) {
        mapKit.setAddressLocation(pos);
    }

    @Deprecated
    public JXMapViewer getMainMap() {
        return mapKit.getMainMap();
    }

    @Deprecated
    public JXMapViewer getMiniMap() {
        return mapKit.getMiniMap();
    }

    @Deprecated
    public JButton getZoomInButton() {
        return mapKit.getZoomInButton();
    }

    @Deprecated
    public JButton getZoomOutButton() {
        return mapKit.getZoomOutButton();
    }

    @Deprecated
    public JSlider getZoomSlider() {
        return mapKit.getZoomSlider();
    }

    @Deprecated
    public void setAddressLocationShown(boolean b) {
        mapKit.setAddressLocationShown(b);
    }

    @Deprecated
    public boolean isAddressLocationShown() {
        return mapKit.isAddressLocationShown();
    }

    @Deprecated
    public void setDataProviderCreditShown(boolean b) {
        mapKit.setDataProviderCreditShown(b);
    }

    @Deprecated
    public boolean isDataProviderCreditShown() {
        return mapKit.isDataProviderCreditShown();
    }

    @Deprecated
    public void setDefaultProvider(DefaultProviders prov) {
        mapKit.setDefaultProvider(prov);
    }

    @Deprecated
    public DefaultProviders getDefaultProvider() {
        return mapKit.getDefaultProvider();
    }
    //</editor-fold>
}
