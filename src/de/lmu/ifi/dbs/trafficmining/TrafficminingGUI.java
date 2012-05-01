package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.clustering.*;
import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.painter.GraphPainter;
import de.lmu.ifi.dbs.trafficmining.painter.NodePainter;
import de.lmu.ifi.dbs.trafficmining.painter.PathPainter;
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
    private final MouseAdapter nodeSetMouseAdapter = new MapToNodeList();
    private final OSMNodeListModel nodeListModel = new OSMNodeListModel();
    //
    private final GraphPainter graphPainter = new GraphPainter();
    private final PathPainter pathPainter = new PathPainter();
    private final NodePainter visitedNodesPainter = new NodePainter();
    private final WaypointPainter<JXMapViewer> startEndPainter = new WaypointPainter<>();
    //
    private final LoadGraphAction loadAction = new LoadGraphAction();
    private final JXMapViewer map;
    private final Map<Integer, SimplexResultEntry> results = new HashMap<>();
    // -
    private Map<Class, String> resultToLayoutName; // cardlayout
    private Map<Class, SimplexControl> resultToSimplexControl;
    //
    private Result result;
    private Statistics statistics;
    private OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph;
    private LoadGraphWorker loadGraphWorker;
    private AlgorithmWorker calculator;
    private StatisticsFrame statisticsFrame;
    private Algorithm currentAlgorithm;
    private HashMap<String, TileServer> tileservers = new HashMap<>();
    private TileServer tileServer;

    public TrafficminingGUI() throws IOException {
        log.info("start");
        initComponents();

        setLocationRelativeTo(null);

        map = jXMapKit.getMainMap();
        properties = new TrafficminingProperties();
        initAlgorithmComboBox();
        initTileServers();
        initResultBindings();
        initClusterComponents();

        restoreLastMapPosition();
        refreshPainters();
        loadRecentGraph();

        resultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                reloadStatisticsData();
                highlightResult();
            }
        });
    }

    private void initClusterComponents() {
        // FIXME : check clustering
        clusterTree.setModel(new ClusterTreeModel());
        clusterTree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent evt) {
                TreePath path = evt.getNewLeadSelectionPath();
                if (path != null) {
                    highlightClusteredRoutes(path.getLastPathComponent());
                }
            }
        });
        ToolTipManager.sharedInstance().registerComponent(clusterTree);
        clusterTree.setCellRenderer(new ClusterTreeCellRenderer());
    }

    private void loadRecentGraph() {
        String dir = properties.getProperty(TrafficminingProperties.lru_graph_dir);
        String file = properties.getProperty(TrafficminingProperties.lru_graph_file);
        boolean autoLoadGraph = properties.getBoolean(TrafficminingProperties.autoload_graph, false);
        autoloadMenuItem.setSelected(autoLoadGraph);
        if (autoLoadGraph && dir != null && file != null) {
            try {
                File osmXml = new File(new File(dir), file);
                loadGraphFromFile(osmXml);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed loading recent graph", e);
            }
        }
    }

    private void initResultBindings() {
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
    }

    private void initTileServers() {
        try {
            TileServerFactory tileServerFactory = new TileServerFactory();
            tileServerFactory.load();
            tileservers = tileServerFactory.getTileServers();
            setTileServer(tileServerFactory.getDefaultServer());
            addTileServerToMenu();

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

    private void addTileServerToMenu() {
        ButtonGroup group = new ButtonGroup();
        List<String> names = new ArrayList<>(tileservers.keySet());
        Collections.sort(names);
        for (String key : names) {
            final TileServer ts = tileservers.get(key);
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(key);
            group.add(menuItem);
            tileserverMenu.add(menuItem);
            menuItem.setSelected(ts.equals(tileServer));
            menuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setTileServer(ts);
                    repaint();
                }
            });
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

    private void startClustering() {
        if (result.getResults().size() > 1) {
            SingleLinkClusteringWithPreprocessing skyclus = new SingleLinkClusteringWithPreprocessing();
            skyclus.setInput(result);
            skyclus.start();
            Cluster cluster = skyclus.getResult();
            if (cluster != null) {
                clusterTree.setModel(new ClusterTreeModel(cluster));
                clusterTree.updateUI();
            }
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
        try {
            Double lat = properties.getDouble(TrafficminingProperties.map_last_center_latitude);
            Double lon = properties.getDouble(TrafficminingProperties.map_last_center_longitude);
            if (lat != null && lon != null) {
                map.setCenterPosition(new GeoPosition(lat, lon));
            }
        } catch (NumberFormatException nfe) {
        }
    }

    private void configureAlgorithm() {
        BeansConfigDialog bcd = null;
        try {
            ensureInitAlgorithm();

            if (currentAlgorithm != null) {
                bcd = new BeansConfigDialog(this, true);
                bcd.setBean(currentAlgorithm);
                bcd.setVisible(true);
            }
        } catch (Exception ex) {
            if (bcd != null) {
                bcd.dispose();
            }
            // tell the user that s.th went wrong
            log.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "The selected algorithm could not be instanciated.\n"
                    + "This can be caused by a faulty plugin:\n"
                    + Arrays2.join(ex.getStackTrace(), "\n") + "\n"
                    + "Maybe the log file is more informative about what went wrong.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        GeoPosition center = map.getCenterPosition();

        properties.setProperty(TrafficminingProperties.map_last_zoom, map.getZoom());
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
        editNodeButton.setEnabled(true);

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
        clusterTree.setModel(null);

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

    private void initAlgorithmComboBox() {
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
                OSMNode node = spf.getNode();
                if (node != null) {
                    nodeListModel.addElement(node);
                    nodeWaypointList.ensureIndexIsVisible(nodeListModel.size() - 1);
                    paintWaypoints(nodeListModel.getWaypoints());

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
            List<Path<?, ? extends OSMNode, ? extends OSMLink>> pathList = new ArrayList<>();
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

        if (nodeListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Route including start and endpoint must be set.");
            return;
        }

        if (editNodeButton.isSelected()) {
            editNodeButton.doClick();
        }
        editNodeButton.setEnabled(false);

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
            editNodeButton.setEnabled(true);
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
            // hold a significant amount of ressources.
            System.gc();
            currentAlgorithm = clazz.newInstance();
        }

        currentAlgorithm.setGraph(graph);
        currentAlgorithm.setNodes(nodeListModel.getNodes());
    }

    private void loadGraphFromFile(File sourceFile) {
        log.fine("starting load graph worker");
        boolean useTagWhitelist = false;
        if (useWhitelistMenuItem.isSelected()) {
            useTagWhitelist = true;
        }

        if (loadGraphWorker != null) {
            loadGraphWorker.cancel(true);
        }
        loadGraphWorker = new LoadGraphWorker(sourceFile, useTagWhitelist);
        loadGraphWorker.addPropertyChangeListener(new LoadGraphListener(this));
        busyLabel.setBusy(true);
        loadGraphWorker.execute();
    }

    void setGraph(OSMGraph graph) {
        this.graph = graph;
        graphLoaded();
    }

    /**
     * Opens the filechooser for the load graph action. Upon File selection, a
     * new SwingWorker ist started to load the files.
     *
     * Also updates the painter as soon as the graph is loaded
     *
     * @TODO extract class
     */
    class LoadGraphAction implements ActionListener {

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
                    return "*.osm";
                }
            });

            // If the user pressed "okay", try to load the files
            if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(getContentPane())) {

                // setting lru dir
                lruDir = chooser.getCurrentDirectory().getAbsolutePath();
                properties.setProperty(TrafficminingProperties.lru_graph_dir, lruDir);

                // setting lru file
                String lruFile = chooser.getSelectedFile().getName();
                properties.setProperty(TrafficminingProperties.lru_graph_file, lruFile);

                try {
                    loadGraphFromFile(chooser.getSelectedFile());
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "couldn't init graph loader:", t.getMessage());
                    JOptionPane.showInternalMessageDialog(getContentPane(), t.getMessage());
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
            if (nodeListModel.contains(node)) {
                nodeListModel.removeElement(node);
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
                nodeListModel.addElement(node);
            }
            nodeWaypointList.ensureIndexIsVisible(nodeListModel.size() - 1);

            paintWaypoints(nodeListModel.getWaypoints());
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
        nodeListModel.removeAllElements();
        paintWaypoints(Collections.EMPTY_LIST);
    }

    /**
     * Removes all selected nodes from the list of waypoints
     */
    private void deleteSelectedNodes() {
        int[] selectedIx = nodeWaypointList.getSelectedIndices();
        for (int i = selectedIx.length - 1; i >= 0; i--) {
            int position = selectedIx[i];
            nodeListModel.remove(position);
        }
        paintWaypoints(nodeListModel.getWaypoints());
    }

    private void toggleEditNodes() {
        if (editNodeButton.isSelected()) {
            clearButton.setEnabled(true);
            adressSearchButton.setEnabled(true);
            nodeWaypointList.setModel(nodeListModel);
            map.addMouseListener(nodeSetMouseAdapter);
        } else {
            clearButton.setEnabled(false);
            adressSearchButton.setEnabled(false);
            map.removeMouseListener(nodeSetMouseAdapter);
        }
    }

    private void paintWaypoints(List<Waypoint> wp) {
        startEndPainter.setWaypoints(new HashSet<>(wp));
        refreshPainters();
    }

    private void openPBFWindow() {
        PbfImportFrame frame = new PbfImportFrame();
        frame.setMapTileServer(tileServer);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addOsmLoadListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                File osmFile = (File) evt.getNewValue();
                if (osmFile.exists() && osmFile.length() > 0) {
                    loadGraphFromFile(osmFile);
                }
            }
        });

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
        waypointPanel = new javax.swing.JPanel();
        jScrollPane_nodes = new javax.swing.JScrollPane();
        nodeWaypointList = new javax.swing.JList();
        editNodeButton = new javax.swing.JToggleButton();
        clearButton = new javax.swing.JButton();
        adressSearchButton = new javax.swing.JButton();
        restultTabPanel = new javax.swing.JTabbedPane();
        javax.swing.JScrollPane resultListTab = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        relustClusterTreeTab = new javax.swing.JScrollPane();
        clusterTree = new javax.swing.JTree();
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
        importPbfMenuItem = new javax.swing.JMenuItem();
        autoloadMenuItem = new javax.swing.JCheckBoxMenuItem();
        useWhitelistMenuItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator = new javax.swing.JPopupMenu.Separator();
        paintGraphMenuItem = new javax.swing.JCheckBoxMenuItem();
        visitedNodesItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        tileserverMenu = new javax.swing.JMenu();
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        leftPanel.add(algorithmPanel, gridBagConstraints);

        waypointPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Waypoints"));
        waypointPanel.setLayout(new java.awt.GridBagLayout());

        jScrollPane_nodes.setAutoscrolls(true);

        nodeWaypointList.setVisibleRowCount(5);
        nodeWaypointList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                nodeWaypointListKeyReleased(evt);
            }
        });
        jScrollPane_nodes.setViewportView(nodeWaypointList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        waypointPanel.add(jScrollPane_nodes, gridBagConstraints);

        editNodeButton.setText("Edit");
        editNodeButton.setEnabled(false);
        editNodeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editNodeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        waypointPanel.add(editNodeButton, gridBagConstraints);

        clearButton.setText("Clear");
        clearButton.setEnabled(false);
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        waypointPanel.add(clearButton, gridBagConstraints);

        adressSearchButton.setText("Search by Address");
        adressSearchButton.setEnabled(false);
        adressSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adressSearchButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        waypointPanel.add(adressSearchButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        leftPanel.add(waypointPanel, gridBagConstraints);

        restultTabPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Result routes"));

        resultListTab.setPreferredSize(new java.awt.Dimension(150, 150));

        resultTable.setAutoCreateRowSorter(true);
        resultTable.setModel(resultTableModel);
        resultTable.setPreferredSize(null);
        resultTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultListTab.setViewportView(resultTable);

        restultTabPanel.addTab("List", resultListTab);

        relustClusterTreeTab.setPreferredSize(new java.awt.Dimension(150, 150));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        clusterTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        clusterTree.setPreferredSize(null);
        relustClusterTreeTab.setViewportView(clusterTree);

        restultTabPanel.addTab("Clustered", relustClusterTreeTab);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.4;
        leftPanel.add(restultTabPanel, gridBagConstraints);

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

        importPbfMenuItem.setText("Convert PBF");
        importPbfMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importPbfMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importPbfMenuItem);

        autoloadMenuItem.setSelected(true);
        autoloadMenuItem.setText("Reload last graph upon start");
        autoloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoloadMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(autoloadMenuItem);

        useWhitelistMenuItem.setSelected(true);
        useWhitelistMenuItem.setText("Only load whitelisted tags");
        fileMenu.add(useWhitelistMenuItem);
        fileMenu.add(jSeparator);

        paintGraphMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        paintGraphMenuItem.setSelected(true);
        paintGraphMenuItem.setText("Paint graph");
        paintGraphMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paintGraphMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(paintGraphMenuItem);

        visitedNodesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        visitedNodesItem.setText("Paint visited nodes");
        visitedNodesItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                visitedNodesItemActionPerformed(evt);
            }
        });
        fileMenu.add(visitedNodesItem);
        fileMenu.add(jSeparator1);

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

        tileserverMenu.setMnemonic('T');
        tileserverMenu.setText("Map View");
        menuBar.add(tileserverMenu);

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

    private void editNodeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editNodeButtonActionPerformed
        toggleEditNodes();
    }//GEN-LAST:event_editNodeButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        clearNodeListModel();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void adressSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adressSearchButtonActionPerformed
        openSeekWindow();
    }//GEN-LAST:event_adressSearchButtonActionPerformed

    private void nodeWaypointListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nodeWaypointListKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelectedNodes();
        }
    }//GEN-LAST:event_nodeWaypointListKeyReleased

private void importPbfMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPbfMenuItemActionPerformed
    openPBFWindow();
}//GEN-LAST:event_importPbfMenuItemActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed

    AboutDialog ad = new AboutDialog(this, true);
    ad.setLocationRelativeTo(null);
    ad.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton adressSearchButton;
    private javax.swing.DefaultComboBoxModel algorithmBoxModel;
    private javax.swing.JComboBox algorithmComboBox;
    private javax.swing.JCheckBoxMenuItem autoloadMenuItem;
    private org.jdesktop.swingx.JXBusyLabel busyLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JTree clusterTree;
    private javax.swing.JButton configureButton;
    private javax.swing.JToggleButton editNodeButton;
    private javax.swing.JMenuItem importPbfMenuItem;
    private javax.swing.JScrollPane jScrollPane_nodes;
    private org.jdesktop.swingx.JXMapKit jXMapKit;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JList nodeWaypointList;
    private javax.swing.JCheckBoxMenuItem paintGraphMenuItem;
    private javax.swing.JScrollPane relustClusterTreeTab;
    private javax.swing.JTabbedPane restultTabPanel;
    private javax.swing.JTable resultTable;
    private javax.swing.table.DefaultTableModel resultTableModel;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JButton searchButton;
    private javax.swing.JPanel simplexContainer;
    private de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl1D simplexControl1D;
    private de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl2D simplexControl2D;
    private de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl3D simplexControl3D;
    private javax.swing.JLabel statusbarLabel;
    private javax.swing.JMenu tileserverMenu;
    private javax.swing.JCheckBoxMenuItem useWhitelistMenuItem;
    private javax.swing.JSplitPane verticalSplitPane;
    private javax.swing.JCheckBoxMenuItem visitedNodesItem;
    private javax.swing.JPanel waypointPanel;
    // End of variables declaration//GEN-END:variables

    public static void main(String args[]) throws Exception {
        try (InputStream is = TrafficminingGUI.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "ERROR: {0}\nlogging.properties not found inside jar!", ex.getMessage());
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    TrafficminingGUI d = new TrafficminingGUI();
                    d.setVisible(true);
                } catch (Throwable ex) {
                    Logger.getLogger(TrafficminingGUI.class.getName()).log(Level.SEVERE, null, ex);
                    System.exit(1);
                }
            }
        });
    }
}

class LoadGraphListener implements PropertyChangeListener {

    private static final Logger log = Logger.getLogger(LoadGraphListener.class.getName());
    private final TrafficminingGUI ui;

    public LoadGraphListener(TrafficminingGUI ui) {
        this.ui = ui;
    }

    /**
     * Method that is called as soon as the graphloader task has finished
     *
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        LoadGraphWorker sw = (LoadGraphWorker) evt.getSource();
        if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
            try {
                if (!sw.isCancelled()) {
                    ui.setGraph(sw.get());
                }
            } catch (InterruptedException | ExecutionException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
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
