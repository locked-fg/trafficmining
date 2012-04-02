package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.clustering.*;
import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.graphpainter.GraphPainter;
import de.lmu.ifi.dbs.trafficmining.graphpainter.NodePainter;
import de.lmu.ifi.dbs.trafficmining.graphpainter.PathPainter;
import de.lmu.ifi.dbs.trafficmining.result.*;
import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils.PATH_ATTRIBUTES;
import de.lmu.ifi.dbs.trafficmining.utils.PluginLoader;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.awt.CardLayout;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreePath;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;

public class TrafficminingGUI extends javax.swing.JFrame {

    private static final Logger log = Logger.getLogger(TrafficminingGUI.class.getName());
    private final TrafficminingProperties properties;
    // map result type -> layout name
    private final Map<Class, String> resultToLayoutName; // cardlayout
    private final Map<Class, SimplexControl> resultToSimplexControl;
    private final MouseAdapter nodeSetMouseAdapter = new MapToNodeList();
    private final OSMNodeListModel model_wp = new OSMNodeListModel();
    private final GraphPainter graphPainter = new GraphPainter();
    private final PathPainter pathPainter = new PathPainter();
    private final LoadGraphAction loadAction = new LoadGraphAction();
    private final NodePainter visitedNodesPainter = new NodePainter();
    private final Map<Integer, SimplexResultEntry> results = new HashMap<>();
    private final WaypointPainter<JXMapViewer> startEndPainter = new WaypointPainter<>();
    // -
    private Result result;
    private Statistics statistics;
    private OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph;
    private LoadGraphWorker loadGraphWorker;
    private AlgorithmWorker calculator;
    private PBFtoOSMFrame pbf_o;
    private StatisticsFrame statisticsFrame;
    private Algorithm currentAlgorithm;
    // fields used for the post processing of nodes
    private ClusterTreeModel clusterModel;
    private Cluster clusterTree;
    private List<TileServer> tileservers = new ArrayList<>();
    private TileServer tileServer;
    private JXMapViewer map;

    public TrafficminingGUI() throws IOException {
        log.info("start");
        initComponents();
        map = jXMapKit.getMainMap();

        properties = new TrafficminingProperties();
        pbf_o = new PBFtoOSMFrame();

        loadAlgorithmComboBox();

        createTileServer();

        restoreLastMapPosition();

        refreshPainters();
        setLocationRelativeTo(null);

        // fill the resultTo*- maps
        Map<Class, String> mapToLayoutName = new HashMap<>();
        mapToLayoutName.put(Simplex1Result.class, "simplex1d");
        mapToLayoutName.put(Simplex2Result.class, "simplex2d");
        mapToLayoutName.put(Simplex3Result.class, "simplex3d");

        resultToLayoutName = Collections.unmodifiableMap(mapToLayoutName);

        // fill the resultTo*- maps
        Map<Class, SimplexControl> mapToControl = new HashMap<>();
        mapToControl.put(Simplex1Result.class, simplexControl1D);
        mapToControl.put(Simplex2Result.class, simplexControl2D);
        mapToControl.put(Simplex3Result.class, simplexControl3D);
        resultToSimplexControl = Collections.unmodifiableMap(mapToControl);

        // register Actionlisterner on SimplexControls' paintpanel to link from the
        // paint panel to the resultList
        simplexControl1D.addMouseListener(new SimplexHighlighter(resultTable));
        simplexControl2D.addMouseListener(new SimplexHighlighter(resultTable));
        simplexControl3D.addMouseListener(new SimplexHighlighter(resultTable));

        // load recent graph
        if (properties.getFile(TrafficminingProperties.plugin_dir) == null) {
            properties.setProperty(TrafficminingProperties.plugin_dir, new File("./"));
            properties.save();
        }

        String lrud = properties.getProperty(TrafficminingProperties.lru_graph_dir);
        String lruf = properties.getProperty(TrafficminingProperties.lru_graph_file);
        boolean autoLoadGraph = properties.getBoolean(TrafficminingProperties.autoload_graph, false);
        autoloadMenuItem.setSelected(autoLoadGraph);
        if (autoLoadGraph && lrud != null && lruf != null) {
            try {
                File osmXml = new File(new File(lrud), lruf);
                loadGraphFromFile(osmXml, loadAction);
            } catch (Exception e) {
                log.log(Level.SEVERE, "", e);
            }
        }

        resultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                reloadStatisticsData();
                highlightResult();
            }
        });

        // FIXME : check clustering
        clusterModel = new ClusterTreeModel(clusterTree);
        jTree_cluster.setModel(clusterModel);
        jTree_cluster.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent evt) {
                TreePath path = evt.getNewLeadSelectionPath();
                if (!(path == null)) {
                    highlightClusteredRoutes(path.getLastPathComponent());
                }
            }
        });
        ToolTipManager.sharedInstance().registerComponent(jTree_cluster);
        jTree_cluster.setCellRenderer(new ClusterTreeCellRenderer());
    }

    /**
     * @fixme servers hardcoded = uncool solution: exclude whole process into
     * some *.properties files
     *
     */
    private void createTileServer() {
        //TODO implement, but read FAQ...
        TileServer ts_osm = new TileServer("osm_mapnik", true, 1, 15, 17, 256, true, true, "http://tile.openstreetmap.org/", "x", "y", "z");
        ts_osm.setCaching(true);
        ts_osm.setLoadBalancing(true, "http://@.tile.openstreetmap.org/", "@", new String[]{"a", "b", "c"});
        ts_osm.setUpTileFactory();

//        TileServer ts_ocm = new TileServer("opencyclemap",true,1,15,16,256,true,true,"http://tile.opencyclemap.org/cycle/","x","y","z");
//        ts_ocm.setCaching(true);
//        ts_ocm.setLoadBalancing(true,"http://@.tile.opencyclemap.org/cycle/","@",new String[]{"a", "b", "c"});
//        ts_ocm.setUpTileFactory();
//        ts_ocm.setVERBOSE(false);

        TileServer ts_mq = new TileServer("mapquest", true, 1, 15, 17, 256, true, true, "http://otile.mqcdn.com/tiles/1.0.0/osm/", "x", "y", "z");
        ts_mq.setCaching(true);
        ts_mq.setLoadBalancing(true, "http://otile@.mqcdn.com/tiles/1.0.0/osm/", "@", new String[]{"1", "2", "3", "4"});
        ts_mq.setUpTileFactory();

        //http://developer.mapquest.com/web/products/open/map
//        TileServer ts_mqoa = new TileServer("mapquest_open_aerial", true, 1, 10, 11, 256, true, true, "http://oatile.mqcdn.com/tiles/1.0.0/sat/", "x", "y", "z");
//        ts_mqoa.setCaching(true);
//        ts_mqoa.setLoadBalancing(true, "http://oatile@.mqcdn.com/tiles/1.0.0/sat/", "@", new String[]{"1", "2", "3", "4"});
//        ts_mqoa.setUpTileFactory();
//        ts_mqoa.setVERBOSE(false);

        //INFO SEE HERE
        //http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Tile_servers
        tileservers.add(ts_osm);
//        TILESERVERS.add(ts_tah);
//        TILESERVERS.add(ts_ocm);
        tileservers.add(ts_mq);
//        TILESERVERS.add(ts_mqoa);
        setTileServer(ts_osm);
        addTileServerToMenu();

        // load more tiles in parallel
        // MIND THE TILE USE POLICY IF USING OSM DIRECTLY
        // http://wiki.openstreetmap.org/wiki/Tile_usage_policy
        ((DefaultTileFactory) map.getTileFactory()).setThreadPoolSize(8);
        map.setRestrictOutsidePanning(true);
        map.setHorizontalWrapped(false);
//        map.setDrawTileBorders(true);
    }

    private void addTileServerToMenu() {
        ButtonGroup button_group = new ButtonGroup();
        for (final TileServer ts : tileservers) {
            JCheckBoxMenuItem rb = new JCheckBoxMenuItem(ts.getName() + " : " + ts.getBaseURL());
            button_group.add(rb);
            jMenu_tileservers.add(rb);
            rb.setSelected(ts.equals(tileServer));
            rb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setTileServer(ts);
                    repaint();
                }
            });
        }
    }

    public boolean setTileServer(TileServer tileServer) {
        if (this.tileServer == tileServer) {
            return true;
        }

        if (tileServer.getTileFactory() != null) {
            if (tileServer.isBroken()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Tileserver \"" + tileServer.getBaseURL() + "\" seems to be broken."
                        + "\nPlease re-check all properties.",
                        "Tileserver is broken.", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Tileserver \"" + tileServer.getBaseURL() + "\" has no own TileFactory set up."
                    + "\nPlease re-check your code and enable it.",
                    "Tileserver not initialized.", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        this.tileServer = tileServer;
        map.setTileFactory(tileServer.getTileFactory());
        return true;
    }

    private void startClustering() {
        if (result.getResults().size() > 1) {
            SingleLinkClusteringWithPreprocessing skyclus = new SingleLinkClusteringWithPreprocessing();
            skyclus.setInput(result);
            skyclus.start();
            clusterTree = skyclus.getResult();
            updateClusterTree();
        }
    }

    private void updateClusterTree() {
        log.fine("Update ClusterTree...");
        if (!(clusterTree == null)) {
            clusterModel = new ClusterTreeModel(clusterTree);
            jTree_cluster.setModel(clusterModel);
            jTree_cluster.updateUI();
        }
    }

    private void highlightClusteredRoutes(Object o) {
        log.fine("Highlight whole cluster...");
        List<Route> list = null;

        if (o instanceof Route) {
            list = new ArrayList<>();
            list.add((Route) o);
        }
        if (o instanceof Cluster) {
            list = ((Cluster) o).getRoutes();
        }

        resultTable.clearSelection();
        for (Route r : list) {
            resultTable.addRowSelectionInterval(r.getId() - 1, r.getId() - 1);
        }
    }

    private void restoreLastMapPosition() {
        //        Integer zoom = properties.getInteger(TrafficminingProperties.map_last_zoom);
        try {
            Double lat = properties.getDouble(TrafficminingProperties.map_last_center_latitude);
            Double lon = properties.getDouble(TrafficminingProperties.map_last_center_longitude);

            //        if (zoom != null && lat != null && lon != null) {
            if (lat != null && lon != null) {
                map.setCenterPosition(new GeoPosition(lat, lon));
                //            map.setZoom(zoom);
            }
        } catch (NumberFormatException nfe) {
        }
    }

    /**
     * Reformats an exception's stacktrace to a string
     *
     * @param e the exception
     * @return string of the stacktraceo
     */
    private String stackTraceToString(Exception e) {
        return Arrays2.join(e.getStackTrace(), "\n");
    }

    private void configureAlgorithm() {
        try {
            ensureInitAlgorithm();
        } catch (InstantiationException | IllegalAccessException ex) {
            // tell the user that s.th went wrong
            log.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "The selected algorithm could not be instanciated.\n"
                    + "This can be caused by a faulty plugin:\n"
                    + stackTraceToString(ex) + "\n"
                    + "Maybe the log file is more informative about what went wrong.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // show config dialog
        if (currentAlgorithm != null) {
            BeansConfigDialog bcd = new BeansConfigDialog(this, true);
            try {
                bcd.setBean(currentAlgorithm);
                bcd.setVisible(true);
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
                bcd.dispose();
                JOptionPane.showMessageDialog(this,
                        "The selected algorithm could not be instanciated.\n"
                        + "This can be caused by a faulty plugin:\n"
                        + stackTraceToString(ex) + "\n"
                        + "Maybe the log file is more informative about what went wrong.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose() {
        GeoPosition center = map.getCenterPosition();
        int zoom = map.getZoom();

        properties.setProperty(TrafficminingProperties.map_last_zoom, zoom);
        properties.setProperty(TrafficminingProperties.map_last_center_latitude, center.getLatitude());
        properties.setProperty(TrafficminingProperties.map_last_center_longitude, center.getLongitude());
        properties.save();

        super.dispose();
    }

    /**
     * called as soon as the graph is loaded
     */
    private void graphLoaded() {
        busyLabel.setBusy(false);
        String statusText = String.format("Finished loading graph. %d links, %d nodes", graph.getLinkCount(), graph.getNodes().size());
        statusbarLabel.setText(statusText);
        graphPainter.setGraph(graph);
        Collection<OSMNode<OSMLink>> nodes = graph.getNodes();
        Set<GeoPosition> geo_set = new HashSet<>();
        for (OSMNode<OSMLink> oSMNode : nodes) {
            geo_set.add(oSMNode.getGeoPosition());
        }
        if (nodes.size() > 0) {
            jXMapKit.setZoom(1);
            map.calculateZoomFrom(geo_set);
        }
        jXMapKit.repaint();
        jToggleButton_editNodes.setEnabled(true);

        clearNodeListModel();
        resetSimplexResultsClustersHighlightedPaths();
    }

    private void resetSimplexResultsClustersHighlightedPaths() {
        // reset all simplex controls
        simplexControl1D.setPoints(Collections.EMPTY_LIST);
        simplexControl2D.setPoints(Collections.EMPTY_LIST);
        simplexControl3D.setPoints(Collections.EMPTY_LIST);

        // reset results
        resultTableModel.setRowCount(0);
        results.clear();

        // reset clusters
        clusterModel = null;
        jTree_cluster.setModel(clusterModel);

        // reset highlighted paths
        pathPainter.clear();
        jXMapKit.repaint();
    }

    private void showStatisticsFrame() {
        if (statisticsFrame == null || !statisticsFrame.isVisible()) {
            statisticsFrame = new StatisticsFrame();
            statisticsFrame.setLocationRelativeTo(null);
        }
        statisticsFrame.setVisible(true);
        reloadStatisticsData();
    }

    private void reloadStatisticsData() {
        if (results.size() <= 0 || statisticsFrame == null || !statisticsFrame.isVisible()) {
            return;
        }

        statisticsFrame.clear();
        statisticsFrame.setData(statistics);

        if (resultTable.getSelectedRowCount() > 0) {
            for (int rowId : resultTable.getSelectedRows()) {
                Integer resultId = (Integer) resultTable.getValueAt(rowId, 0);
                SimplexResultEntry entry = results.get(resultId);
                if (entry != null) {
                    Map<PATH_ATTRIBUTES, String> mapIntern = statistics.getPath(entry.getPath());
                    statisticsFrame.addPathData(mapIntern, resultId);
                }
            }
        }
    }

    private void loadAlgorithmComboBox() {
        File pluginDir = properties.getFile(TrafficminingProperties.plugin_dir);
        new ComboboxLoader(algorithmBoxModel, pluginDir).load();
        if (algorithmBoxModel.getSize() > 0) {
            configureButton.setEnabled(true);
            searchButton.setEnabled(true);
            algorithmComboBox.setSelectedIndex(0);
        }
    }

    private void openSeekWindow() {
        if (graph == null) {
            return;
        }

        GeoPosition aPosition = map.getCenterPosition();
        final SeekPositionFrame spf = new SeekPositionFrame(this.graph, aPosition, tileServer);
        spf.setVisible(true);
        WindowAdapter wa = new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                SeekPositionFrame spf = (SeekPositionFrame) e.getWindow();
                spf.removeWindowListener(this);
                OSMNode spf_found = spf.getNode();
                if (spf_found != null) {
//                    GeoPosition coord = spf.getGeoPos();

                    model_wp.addElement(spf_found);

                    jList_nodes.ensureIndexIsVisible(model_wp.size() - 1);
                    paintWaypoints(model_wp.getWaypoints());

                }
            }
        };
        spf.addWindowListener(wa);
    }

    private void refreshPainters() {
        List<Painter> list = new ArrayList<>();
        if (paintGraphMenuItem.isSelected()) {
            list.add(graphPainter);
        }
        list.add(visitedNodesPainter);
        list.add(pathPainter);
        list.add(startEndPainter);
        map.setOverlayPainter(new CompoundPainter(list.toArray(new Painter[]{})));
    }

    private void highlightResult() {
        int[] rowIDs = resultTable.getSelectedRows();
        if (rowIDs.length == 0 || results.isEmpty() || resultTable.getRowCount() == 0) {
            return;
        }

        List<SimplexResultEntry> items = new ArrayList<>();
        for (int rowID : rowIDs) {
            rowID = resultTable.convertRowIndexToModel(rowID);
            Integer id = (Integer) resultTableModel.getValueAt(rowID, 0);
            if (results.containsKey(id)) {
                items.add(results.get(id));
            }
        }

        // okay, this call IS ugly
        Class resultClass = results.values().iterator().next().getResult().getClass();
        SimplexControl simplexControl = resultToSimplexControl.get(resultClass);
        assert simplexControl != null;
        List<PointSource> list = new ArrayList<>();
        if (items.isEmpty()) {
            pathPainter.clear();
        } else {
            List<Path<?, ? extends OSMNode, ?>> pathList = new ArrayList<>();
            for (SimplexResultEntry resultEntry : items) {
                pathList.add(resultEntry.getPath());
                list.add(resultEntry);
                log.log(Level.INFO, "highlighted: {0}", resultEntry.getPath().toString());
            }
            pathPainter.setPath(pathList);
        }
        simplexControl.setHighlight(list);
        jXMapKit.repaint();
    }

    private void startAndRunAlgorithm() {
        resultTableModel.setRowCount(0);
        simplexControl1D.setPoints(Collections.EMPTY_LIST);
        simplexControl2D.setPoints(Collections.EMPTY_LIST);
        simplexControl3D.setPoints(Collections.EMPTY_LIST);
        pathPainter.setPath(Collections.EMPTY_LIST);
        visitedNodesPainter.setNodes(Collections.EMPTY_LIST);
        repaint();

        if (graph == null) {
            JOptionPane.showMessageDialog(this, "Please load graph first.");
            return;
        }

        if (model_wp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Route including start and endpoint must be set.");
            return;
        }

        if (jToggleButton_editNodes.isSelected()) {
            jToggleButton_editNodes.doClick();
        }
        jToggleButton_editNodes.setEnabled(false);

        // cancel possibly running algorithm
        if (calculator != null && !calculator.isDone()) {
            resultTableModel.setRowCount(0);
            calculator.cancel(true);
            calculator = null;
        }

        busyLabel.setBusy(false);
        try {
            ensureInitAlgorithm();
            calculator = new AlgorithmWorker(currentAlgorithm);
            calculator.addPropertyChangeListener(new AlgorithmWorkerListener(this));
            cancelButton.setEnabled(true);
            busyLabel.setBusy(true);
            calculator.execute();
        } catch (InstantiationException | IllegalAccessException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            log.log(Level.SEVERE, null, ex);
            busyLabel.setBusy(false);
        } finally {
            jToggleButton_editNodes.setEnabled(true);
        }
    }

    private void processResult(AlgorithmResult algorithmResult) {
        result = algorithmResult.getResult();
        statistics = algorithmResult.getStatistics();
        if (result == null) {
            return;
        }

        // Set the correct card
        CardLayout layout = (CardLayout) simplexContainer.getLayout();
        layout.show(simplexContainer, resultToLayoutName.get(result.getClass()));

        resetSimplexResultsClustersHighlightedPaths();

        Map<Path, double[]> resultsMap = result.getResults();
        // Set column names
        Vector colNames = new Vector(result.getUnits());
        colNames.add(0, "route");
        resultTableModel.setColumnIdentifiers(colNames);

        {// set columns sortable
            TableRowSorter sorter = new TableRowSorter(resultTableModel);
            resultTable.setRowSorter(sorter);
            for (int i = 0; i < resultTableModel.getColumnCount(); i++) {
                sorter.setComparator(i, new ResultTableColumnSorter());
            }
        }

        double[] ranges = findMaxPerColumn(resultsMap.values());
        List<PointSource> pointSourceList = new ArrayList<>();
        int i = 1;
        for (Map.Entry<Path, double[]> entry : resultsMap.entrySet()) {
            double[] absolute = entry.getValue().clone();
            double[] relative = entry.getValue().clone();
            Arrays2.div(relative, ranges);
            Arrays2.replaceNaN(relative, 0); // ranges MAY contain zeros


            int resultDimensionality = result.getAttributes().size();
            SimplexResultEntry resEntry = new SimplexResultEntry(result, entry.getKey(), relative, absolute, i++, resultDimensionality);
            results.put(resEntry.getId(), resEntry);
            resultTableModel.addRow(resEntry.getVector());
            pointSourceList.add(resEntry);
        }

        SimplexControl simplexControl = resultToSimplexControl.get(result.getClass());
        simplexControl.setAttributNames(result.getAttributes());
        simplexControl.setPoints(pointSourceList);

        if (resultsMap.size() > 0) {
            resultTable.getSelectionModel().clearSelection();
            resultTable.getSelectionModel().setSelectionInterval(0, 0);
        }
        if (statisticsFrame != null && statisticsFrame.isVisible()) {
            showStatisticsFrame();
        }
        showHideVisitedNodes();

        // CLUSTER ALL RESULTS!
        startClustering();
    }

    private void showHideVisitedNodes() {
        visitedNodesPainter.clear();
        if (statistics != null && visitedNodesItem.isSelected()) {
            visitedNodesPainter.setNodes(statistics.getVisitedNodes());
        }
        jXMapKit.repaint();
    }

    private double[] findMaxPerColumn(Collection<double[]> entries) {
        double[] maxima = null;
        for (double[] e : entries) {
            if (maxima == null) {
                maxima = e.clone();
            }
            for (int i = 0; i < e.length; i++) {
                maxima[i] = Math.max(maxima[i], e[i]);
            }
        }
        return maxima;
    }

    /**
     * Ensures that currentAlgorithm is set and initialized. The method also
     * sets the current graph and the node list.
     *
     * For any configuration beyond this basic setting, call
     * #configureAlgorithm()
     *
     * @throws InstantiationException if the algorithm could not be instanciated
     * @throws IllegalAccessException if the algorithm could not be instanciated
     */
    private void ensureInitAlgorithm() throws InstantiationException,
            IllegalAccessException {
        final Object selectedItem = algorithmComboBox.getSelectedItem();

        if (selectedItem == null || !AlgorithmComboBoxElement.class.isAssignableFrom(selectedItem.getClass())) {
            log.log(Level.WARNING, "item not a combobox element: {0}", selectedItem);
            return;
        }
        final AlgorithmComboBoxElement boxElement = (AlgorithmComboBoxElement) selectedItem;
        final Class<Algorithm> clazz = boxElement.getAlgorithm();

        // new algorithm or same as previous?
        if (currentAlgorithm == null || !currentAlgorithm.getClass().equals(clazz)) {
            currentAlgorithm = null;
            // Propose a GC explicitly because a previous algorithm MIGHT
            // hold a significant amount of ressources. Keep in mind that
            // this does not FORCE a GC (see the API of System.gc()).
            System.gc();
            System.gc();
            System.gc();
            currentAlgorithm = clazz.newInstance();
        }

        currentAlgorithm.setGraph(graph);
        currentAlgorithm.setNodes(model_wp.getNodes());
    }

    private void loadGraphFromFile(File sourceFile, LoadGraphAction listener) {
        log.fine("starting load graph worker");
        boolean useTagWhitelist = false;
        if (useWhitelistMenuItem.isSelected()) {
            useTagWhitelist = true;
        }

        if (loadGraphWorker != null) {
            loadGraphWorker.cancel(true);
        }

        loadGraphWorker = new LoadGraphWorker(sourceFile, useTagWhitelist);
        loadGraphWorker.addPropertyChangeListener(listener);
        loadGraphWorker.execute();
    }

    /**
     * Opens the filechooser for the load graph action. Upon File selection, a
     * new SwingWorker ist started to load the files.
     *
     * Also updates the painter as soon as the graph is loaded
     *
     * @TODO extract class
     */
    class LoadGraphAction implements ActionListener, PropertyChangeListener {

        @Override
        public void actionPerformed(final ActionEvent evt) {
            if (loadGraphWorker != null) {
                log.fine("canceling active worker");
                loadGraphWorker.cancel(true);
            }

            // opening the FileChooser MAY take some ugly time which freezes the GUI :-/
            // if this is an issue, put this into an invokeLater()
            String lruDir = properties.getProperty(TrafficminingProperties.lru_graph_dir);
            JFileChooser chooser = new JFileChooser(lruDir);
            chooser.setMultiSelectionEnabled(false);

            // Display only directories and osm files
            chooser.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File f) {

                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".osm");
                }

                @Override
                public String getDescription() {
                    return "*.osm - OpenStreetMap";
                }
            });

            // If the user pressed "okay", try to load the files
            if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(getContentPane())) {

                // setting lru dir
                lruDir = chooser.getCurrentDirectory().getAbsolutePath();
                properties.setProperty(TrafficminingProperties.lru_graph_dir, lruDir);
                log.log(Level.FINE, "saving least recently used dir: {0}", lruDir);
                // setting lru file
                String lruFile = chooser.getSelectedFile().getName();
                properties.setProperty(TrafficminingProperties.lru_graph_file, lruFile);
                log.log(Level.FINE, "saving least recently used file: {0}", lruFile);
                properties.save();


                if (chooser.getSelectedFiles().length > 1) {
                    JOptionPane.showInternalMessageDialog(getContentPane(), "You must not load more than one *.osm file!");
                    return;
                }

                try {
                    busyLabel.setBusy(true);
                    loadGraphFromFile(chooser.getSelectedFile(), this);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "couldn't init graph loader:", t.getMessage());
                    JOptionPane.showInternalMessageDialog(getContentPane(), t.getMessage());
                }
            }
        }

        /**
         * Method that is called as soon as the graphloader task has finished
         *
         * @param evt
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
                try {
                    if (!loadGraphWorker.isCancelled()) {
                        graph = loadGraphWorker.get();
                        graphLoaded();
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    log.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * MouseAdapter that takes a click on the main map, seeks the nearest node
     * to the click position in the graph and adds this node in the according
     * lists.
     */
    class MapToNodeList extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            GeoPosition pos = map.convertPointToGeoPosition(e.getPoint());
            OSMNode node = OSMUtils.getNearestNode(pos, graph);
            if (model_wp.contains(node)) {
                model_wp.removeElement(node);
            } else {
                List<OSMLink> links = node.getLinks();
                String name = new String();
                for (OSMLink oSMLink : links) {
                    String l_name = oSMLink.getAttr("name");
                    if (l_name != null && !name.contains(l_name)) {
                        name += l_name + "; ";
                    }
                }
                node.setName(name);
                model_wp.addElement(node);
            }
            jList_nodes.ensureIndexIsVisible(model_wp.size() - 1);

            paintWaypoints(model_wp.getWaypoints());
        }
    }

    /**
     * Listener that keeps an eye on an AlgorithmWorker and propagates the
     * result of the computation
     */
    class AlgorithmWorkerListener implements PropertyChangeListener {

        private final JFrame parent;

        /**
         * the parent frame which is needed to display a potential error message
         */
        public AlgorithmWorkerListener(JFrame parent) {
            this.parent = parent;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!AlgorithmWorker.class.isAssignableFrom(evt.getSource().getClass())) {
                return;
            }
            AlgorithmWorker worker = (AlgorithmWorker) evt.getSource();
            if (evt.getPropertyName().equals("state") && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                calculator = null;
                busyLabel.setBusy(false);
                cancelButton.setEnabled(false);
                try {
                    if (!worker.isCancelled()) {
                        processResult(worker.get());
                    }
                } catch (InterruptedException | ExecutionException t) {
                    log.log(Level.SEVERE, "Algorithm terminated by uncaught exception:", t);
                    JOptionPane.showMessageDialog(parent, "Exception in Calculator: " + t.getMessage());
                } finally {
                    worker.removePropertyChangeListener(this);
                }
            }
        }
    }

    /**
     * Removes all nodes from the list of waypoints
     */
    private void clearNodeListModel() {
        model_wp.removeAllElements();
        paintWaypoints(Collections.EMPTY_LIST);
    }

    /**
     * Removes all selected nodes from the list of waypoints
     */
    private void deleteSelectedNodes() {
        int[] selectedIx = jList_nodes.getSelectedIndices();
        for (int i = selectedIx.length - 1; i >= 0; i--) {
            int position = selectedIx[i];
            model_wp.remove(position);
        }
        paintWaypoints(model_wp.getWaypoints());
    }

    private void toggleEditNodes() {
        if (jToggleButton_editNodes.isSelected()) {
            jButton_deleteAllNodes.setEnabled(true);
            jButton_adressSearch.setEnabled(true);
            jList_nodes.setModel(model_wp);
            map.addMouseListener(nodeSetMouseAdapter);
        } else {
            jButton_deleteAllNodes.setEnabled(false);
            jButton_adressSearch.setEnabled(false);
            map.removeMouseListener(nodeSetMouseAdapter);
        }
    }

    private void paintWaypoints(List<Waypoint> wp) {
        startEndPainter.setWaypoints(new HashSet<>(wp));
        refreshPainters();
    }

    private void openPBFWindow() {
        pbf_o.setMapTileServer(tileServer);
        pbf_o.setLocationRelativeTo(null);
        pbf_o.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        algorithmBoxModel = new javax.swing.DefaultComboBoxModel();
        resultTableModel = new de.lmu.ifi.dbs.trafficmining.ReadOnlyTableModel();
        javax.swing.JSplitPane horizontalSplit = new javax.swing.JSplitPane();
        verticalSplitPane = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        javax.swing.JPanel algorithmPanel = new javax.swing.JPanel();
        searchButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        busyLabel = new org.jdesktop.swingx.JXBusyLabel();
        algorithmComboBox = new javax.swing.JComboBox();
        configureButton = new javax.swing.JButton();
        jPanel_nodes = new javax.swing.JPanel();
        jScrollPane_nodes = new javax.swing.JScrollPane();
        jList_nodes = new javax.swing.JList();
        jToggleButton_editNodes = new javax.swing.JToggleButton();
        jButton_deleteAllNodes = new javax.swing.JButton();
        jButton_adressSearch = new javax.swing.JButton();
        jTabbedPane_flat_tree = new javax.swing.JTabbedPane();
        javax.swing.JScrollPane resultTableScrollPane = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        jScrollPane_tree = new javax.swing.JScrollPane();
        jTree_cluster = new javax.swing.JTree();
        javax.swing.JButton showStatisticsButton = new javax.swing.JButton();
        simplexContainer = new javax.swing.JPanel();
        simplexControl1D = new de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl1D();
        simplexControl2D = new de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl2D();
        simplexControl3D = new de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl3D();
        rightPanel = new javax.swing.JPanel();
        jXMapKit = new org.jdesktop.swingx.JXMapKit();
        javax.swing.JPanel statusBar = new javax.swing.JPanel();
        statusbarLabel = new javax.swing.JLabel();
        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem loadGraphItem = new javax.swing.JMenuItem();
        autoloadMenuItem = new javax.swing.JCheckBoxMenuItem();
        useWhitelistMenuItem = new javax.swing.JCheckBoxMenuItem();
        paintGraphMenuItem = new javax.swing.JCheckBoxMenuItem();
        visitedNodesItem = new javax.swing.JCheckBoxMenuItem();
        importPbfMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        clusterMenu = new javax.swing.JMenu();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItem2 = new javax.swing.JCheckBoxMenuItem();
        jMenu_tileservers = new javax.swing.JMenu();
        javax.swing.JMenu aboutMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MARiO: Multi Attribute Routing in Open Street Map");

        horizontalSplit.setDividerLocation(275);
        horizontalSplit.setDividerSize(7);

        verticalSplitPane.setDividerLocation(440);
        verticalSplitPane.setDividerSize(7);
        verticalSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane.setResizeWeight(0.5);
        verticalSplitPane.setPreferredSize(new java.awt.Dimension(200, 600));

        leftPanel.setLayout(new java.awt.GridBagLayout());

        algorithmPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithm"));
        algorithmPanel.setLayout(new java.awt.GridBagLayout());

        searchButton.setText("Execute");
        searchButton.setEnabled(false);
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 2);
        algorithmPanel.add(searchButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 2);
        algorithmPanel.add(cancelButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 0);
        algorithmPanel.add(busyLabel, gridBagConstraints);

        algorithmComboBox.setModel(algorithmBoxModel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        algorithmPanel.add(algorithmComboBox, gridBagConstraints);

        configureButton.setText("...");
        configureButton.setToolTipText("Configure algorithm");
        configureButton.setEnabled(false);
        configureButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        configureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configureButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 1, 0);
        algorithmPanel.add(configureButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        leftPanel.add(algorithmPanel, gridBagConstraints);

        jPanel_nodes.setBorder(javax.swing.BorderFactory.createTitledBorder("Waypoints"));
        jPanel_nodes.setLayout(new java.awt.GridBagLayout());

        jScrollPane_nodes.setAutoscrolls(true);

        jList_nodes.setVisibleRowCount(5);
        jList_nodes.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jList_nodesKeyReleased(evt);
            }
        });
        jScrollPane_nodes.setViewportView(jList_nodes);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel_nodes.add(jScrollPane_nodes, gridBagConstraints);

        jToggleButton_editNodes.setText("edit");
        jToggleButton_editNodes.setEnabled(false);
        jToggleButton_editNodes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton_editNodesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel_nodes.add(jToggleButton_editNodes, gridBagConstraints);

        jButton_deleteAllNodes.setText("clear");
        jButton_deleteAllNodes.setEnabled(false);
        jButton_deleteAllNodes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_deleteAllNodesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel_nodes.add(jButton_deleteAllNodes, gridBagConstraints);

        jButton_adressSearch.setText("address");
        jButton_adressSearch.setEnabled(false);
        jButton_adressSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_adressSearchActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel_nodes.add(jButton_adressSearch, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        leftPanel.add(jPanel_nodes, gridBagConstraints);

        jTabbedPane_flat_tree.setBorder(javax.swing.BorderFactory.createTitledBorder("Routes"));

        resultTableScrollPane.setPreferredSize(new java.awt.Dimension(150, 150));

        resultTable.setAutoCreateRowSorter(true);
        resultTable.setModel(resultTableModel);
        resultTable.setPreferredSize(null);
        resultTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultTableScrollPane.setViewportView(resultTable);

        jTabbedPane_flat_tree.addTab("List", resultTableScrollPane);

        jScrollPane_tree.setPreferredSize(new java.awt.Dimension(150, 150));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree_cluster.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree_cluster.setPreferredSize(null);
        jScrollPane_tree.setViewportView(jTree_cluster);

        jTabbedPane_flat_tree.addTab("Clustered", jScrollPane_tree);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.4;
        leftPanel.add(jTabbedPane_flat_tree, gridBagConstraints);

        showStatisticsButton.setText("show Statistics");
        showStatisticsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showStatisticsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        leftPanel.add(showStatisticsButton, gridBagConstraints);

        verticalSplitPane.setTopComponent(leftPanel);

        simplexContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        simplexContainer.setMinimumSize(new java.awt.Dimension(150, 150));
        simplexContainer.setPreferredSize(new java.awt.Dimension(200, 200));
        simplexContainer.setLayout(new java.awt.CardLayout());
        simplexContainer.add(simplexControl1D, "simplex1d");
        simplexContainer.add(simplexControl2D, "simplex2d");
        simplexContainer.add(simplexControl3D, "simplex3d");

        verticalSplitPane.setBottomComponent(simplexContainer);

        horizontalSplit.setLeftComponent(verticalSplitPane);

        rightPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        rightPanel.setLayout(new java.awt.BorderLayout());

        jXMapKit.setDefaultProvider(org.jdesktop.swingx.JXMapKit.DefaultProviders.OpenStreetMaps);
        jXMapKit.setAddressLocationShown(false);
        jXMapKit.setZoom(15);
        rightPanel.add(jXMapKit, java.awt.BorderLayout.CENTER);

        horizontalSplit.setRightComponent(rightPanel);

        getContentPane().add(horizontalSplit, java.awt.BorderLayout.CENTER);

        statusBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        statusBar.setLayout(new java.awt.GridBagLayout());

        statusbarLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        statusBar.add(statusbarLabel, gridBagConstraints);

        getContentPane().add(statusBar, java.awt.BorderLayout.SOUTH);

        fileMenu.setMnemonic('f');
        fileMenu.setText("File");

        loadGraphItem.addActionListener(loadAction);
        loadGraphItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        loadGraphItem.setText("Load Graph");
        fileMenu.add(loadGraphItem);

        autoloadMenuItem.setSelected(true);
        autoloadMenuItem.setText("autoload graph next time");
        autoloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoloadMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(autoloadMenuItem);

        useWhitelistMenuItem.setSelected(true);
        useWhitelistMenuItem.setText("use whitelist for osm graph tags");
        fileMenu.add(useWhitelistMenuItem);

        paintGraphMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        paintGraphMenuItem.setSelected(true);
        paintGraphMenuItem.setText("paint Graph");
        paintGraphMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paintGraphMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(paintGraphMenuItem);

        visitedNodesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        visitedNodesItem.setText("paint visited nodes");
        visitedNodesItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                visitedNodesItemActionPerformed(evt);
            }
        });
        fileMenu.add(visitedNodesItem);

        importPbfMenuItem.setText("Import PBF");
        importPbfMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importPbfMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importPbfMenuItem);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        clusterMenu.setMnemonic('c');
        clusterMenu.setText("Cluster");
        clusterMenu.setEnabled(false);

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");
        clusterMenu.add(jCheckBoxMenuItem1);

        jCheckBoxMenuItem2.setText("TEST CLUSTERING");
        jCheckBoxMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem2ActionPerformed(evt);
            }
        });
        clusterMenu.add(jCheckBoxMenuItem2);

        menuBar.add(clusterMenu);

        jMenu_tileservers.setMnemonic('T');
        jMenu_tileservers.setText("Tileservers");
        menuBar.add(jMenu_tileservers);

        aboutMenu.setMnemonic('a');
        aboutMenu.setText("About");
        aboutMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuActionPerformed(evt);
            }
        });

        aboutMenuItem.setMnemonic('a');
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        aboutMenu.add(aboutMenuItem);

        menuBar.add(aboutMenu);

        setJMenuBar(menuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        dispose();
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * user selected something in the algorithm list
     */
    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        startAndRunAlgorithm();
    }//GEN-LAST:event_searchButtonActionPerformed

    private void paintGraphMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paintGraphMenuItemActionPerformed
        refreshPainters();
    }//GEN-LAST:event_paintGraphMenuItemActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        if (calculator != null) {
            calculator.cancel(true);
        }
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void autoloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoloadMenuItemActionPerformed
        properties.setProperty(TrafficminingProperties.autoload_graph, autoloadMenuItem.isSelected());
        properties.save();
    }//GEN-LAST:event_autoloadMenuItemActionPerformed

    private void showStatisticsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showStatisticsButtonActionPerformed
        showStatisticsFrame();
    }//GEN-LAST:event_showStatisticsButtonActionPerformed

    private void visitedNodesItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_visitedNodesItemActionPerformed
        showHideVisitedNodes();
    }//GEN-LAST:event_visitedNodesItemActionPerformed

    private void aboutMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuActionPerformed
        AboutDialog dialog = new AboutDialog(this, true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }//GEN-LAST:event_aboutMenuActionPerformed

    private void configureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureButtonActionPerformed
        // don't block the EDT
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                configureAlgorithm();
            }
        });
    }//GEN-LAST:event_configureButtonActionPerformed

    private void jToggleButton_editNodesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton_editNodesActionPerformed
        toggleEditNodes();
    }//GEN-LAST:event_jToggleButton_editNodesActionPerformed

    private void jButton_deleteAllNodesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_deleteAllNodesActionPerformed
        clearNodeListModel();
    }//GEN-LAST:event_jButton_deleteAllNodesActionPerformed

    private void jButton_adressSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_adressSearchActionPerformed
        openSeekWindow();
    }//GEN-LAST:event_jButton_adressSearchActionPerformed

    private void jCheckBoxMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem2ActionPerformed
        if (jCheckBoxMenuItem2.isSelected()) {
            startClustering();
        }
    }//GEN-LAST:event_jCheckBoxMenuItem2ActionPerformed

    private void jList_nodesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jList_nodesKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelectedNodes();
        }
    }//GEN-LAST:event_jList_nodesKeyReleased

private void importPbfMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPbfMenuItemActionPerformed
    openPBFWindow();
}//GEN-LAST:event_importPbfMenuItemActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed

    AboutDialog ad = new AboutDialog(this, true);
    ad.setLocationRelativeTo(null);
    ad.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.DefaultComboBoxModel algorithmBoxModel;
    private javax.swing.JComboBox algorithmComboBox;
    private javax.swing.JCheckBoxMenuItem autoloadMenuItem;
    private org.jdesktop.swingx.JXBusyLabel busyLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JMenu clusterMenu;
    private javax.swing.JButton configureButton;
    private javax.swing.JMenuItem importPbfMenuItem;
    private javax.swing.JButton jButton_adressSearch;
    private javax.swing.JButton jButton_deleteAllNodes;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem2;
    private javax.swing.JList jList_nodes;
    private javax.swing.JMenu jMenu_tileservers;
    private javax.swing.JPanel jPanel_nodes;
    private javax.swing.JScrollPane jScrollPane_nodes;
    private javax.swing.JScrollPane jScrollPane_tree;
    private javax.swing.JTabbedPane jTabbedPane_flat_tree;
    private javax.swing.JToggleButton jToggleButton_editNodes;
    private javax.swing.JTree jTree_cluster;
    private org.jdesktop.swingx.JXMapKit jXMapKit;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JCheckBoxMenuItem paintGraphMenuItem;
    private javax.swing.JTable resultTable;
    private javax.swing.table.DefaultTableModel resultTableModel;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JButton searchButton;
    private javax.swing.JPanel simplexContainer;
    private de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl1D simplexControl1D;
    private de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl2D simplexControl2D;
    private de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl3D simplexControl3D;
    private javax.swing.JLabel statusbarLabel;
    private static javax.swing.JCheckBoxMenuItem useWhitelistMenuItem;
    private javax.swing.JSplitPane verticalSplitPane;
    private javax.swing.JCheckBoxMenuItem visitedNodesItem;
    // End of variables declaration//GEN-END:variables

    public static void main(String args[]) throws Exception {
        try (InputStream is = TrafficminingGUI.class.getResourceAsStream("/logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                is.close();
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, "ERROR: {0}\nlogging.properties not found inside jar!", ex);
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    TrafficminingGUI d = new TrafficminingGUI();
                    d.setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(TrafficminingGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}

class ComboboxLoader {

    private final static Logger logger = Logger.getLogger(ComboboxLoader.class.getName());
    private final DefaultComboBoxModel model;
    private final File pluginDir;

    ComboboxLoader(DefaultComboBoxModel algorithmBoxModel, File pluginDir) {
        this.model = algorithmBoxModel;
        this.pluginDir = pluginDir;
    }

    void load() {
        try {
            model.removeAllElements();
            if (pluginDir == null) {
                logger.log(Level.INFO, "plugin.dir null");
                return;
            }
            if (!pluginDir.exists() || !pluginDir.canRead()) {
                logger.log(Level.INFO, "plugin.dir set but does not exist or is not readable: {0}", pluginDir);
                return;
            }

            PluginLoader<Algorithm> pluginLoader = new PluginLoader<>(pluginDir, Algorithm.class);
            List<Entry<Class<Algorithm>, File>> map = pluginLoader.getMap();
            List<AlgorithmComboBoxElement> list = new ArrayList<>();
            for (Entry<Class<Algorithm>, File> entry : map) {
                try {
                    // FIXME: check length of strings and use substrings if too long
                    // reason: too long string will break the GridBagLayout
                    list.add(new AlgorithmComboBoxElement(entry.getValue(), entry.getKey()));
                } catch (InstantiationException ex) {
                    logger.log(Level.SEVERE, "tried to instanciate an uninstanciable class", ex);
                }
            }
            Collections.sort(list);
            for (AlgorithmComboBoxElement elem : list) {
                model.addElement(elem);
            }
        } catch (IOException | IllegalAccessException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
