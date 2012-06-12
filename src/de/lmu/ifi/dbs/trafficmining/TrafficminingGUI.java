package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.ui.TileServerFactory;
import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.clustering.*;
import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.result.*;
import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl;
import de.lmu.ifi.dbs.trafficmining.ui.AboutDialog;
import de.lmu.ifi.dbs.trafficmining.ui.AlgorithmComboBoxElement;
import de.lmu.ifi.dbs.trafficmining.ui.AlgorithmComboboxLoader;
import de.lmu.ifi.dbs.trafficmining.ui.BeansConfigDialog;
import de.lmu.ifi.dbs.trafficmining.ui.EnableTileserverAction;
import de.lmu.ifi.dbs.trafficmining.ui.PbfImportFrame;
import de.lmu.ifi.dbs.trafficmining.ui.StatisticsFrame;
import de.lmu.ifi.dbs.trafficmining.ui.nodelist.NodeList2MapConnector;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.awt.CardLayout;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class TrafficminingGUI extends javax.swing.JFrame {

    private static final Logger log = Logger.getLogger(TrafficminingGUI.class.getName());
    private final TrafficminingProperties properties;
    private final NodeList2MapConnector node2mapConnector;
    //
    private final LoadGraphAction loadAction = new LoadGraphAction();
    private final Map<Integer, SimplexResultEntry> results = new HashMap<>();
    // -
    private Map<Class, String> resultToLayoutName; // cardlayout
    private Map<Class, SimplexControl> resultToSimplexControl;
    //
    private Result result;
    private Statistics statistics;
    private Graph<Node<Link>, Link<Node>> graph;
    private LoadGraphWorker loadGraphWorker;
    private AlgorithmWorker calculator;
    private StatisticsFrame statisticsFrame;
    private Algorithm currentAlgorithm;

    public TrafficminingGUI() throws IOException {
        log.info("start");
        initComponents();

        this.properties = new TrafficminingProperties();
        this.node2mapConnector = new NodeList2MapConnector(nodeListPanel, mapWrapper);
        setLocationRelativeTo(null);

        initAlgorithmComboBox();
        initResultBindings();
        initClusterComponents();

        restoreLastMapPosition();
        maybeLoadRecentGraph();

        resultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                reloadStatisticsData();
                highlightResult();
            }
        });

        TileServerFactory.get();
        initTileServerMenu();
    }

    private void initTileServerMenu() {
        ButtonGroup group = new ButtonGroup();
        List<String> names = new ArrayList<>(mapWrapper.getTileServers());
        Collections.sort(names);
        for (final String key : names) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(key);
            tileserverMenu.add(menuItem);
            menuItem.setSelected(key.equals(mapWrapper.currentTileserverName()));
            menuItem.addActionListener(new EnableTileserverAction(key, mapWrapper));
            group.add(menuItem);
        }
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

    private void maybeLoadRecentGraph() {
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
            if (lat != null && lon != null && lat != 1 && lon != 1) {
                mapWrapper.setCenterPosition(new GeoPosition(lat, lon));
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

    private void saveProperties() {
        GeoPosition center = mapWrapper.getCenterPosition();

        properties.setProperty(TrafficminingProperties.map_last_zoom, mapWrapper.getZoom());
        properties.setProperty(TrafficminingProperties.map_last_center_latitude, center.getLatitude());
        properties.setProperty(TrafficminingProperties.map_last_center_longitude, center.getLongitude());
        properties.save();
    }

    /**
     * called as soon as the graph is loaded
     */
    private void graphLoaded() {
        busyLabel.setBusy(false);
        String statusText = String.format("Finished loading graph. %d links, %d nodes", graph.getLinkCount(), graph.getNodes().size());
        statusbarLabel.setText(statusText);
        node2mapConnector.setGraph(graph);

        mapWrapper.paintGraph(null);
        if (paintGraphMenuItem.isSelected()) {
            mapWrapper.paintGraph(graph);
        }

        Collection<Node<Link>> nodes = graph.getNodes();
        Set<GeoPosition> geo_set = new HashSet<>();
        for (Node<Link> oSMNode : nodes) {
            geo_set.add(oSMNode.getGeoPosition());
        }
        if (nodes.size() > 0) {
            mapWrapper.setZoom(1);
            mapWrapper.calculateZoomFrom(geo_set);
        }
        mapWrapper.repaint();

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
        mapWrapper.paintPaths(Collections.EMPTY_LIST);
        mapWrapper.repaint();
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
                    Map<String, String> mapIntern = statistics.getPath(entry.getPath());
                    statisticsFrame.addPathData(mapIntern, resultId);
                }
            }
        }
    }

    private void initAlgorithmComboBox() {
        File pluginDir = properties.getFile(TrafficminingProperties.plugin_dir);
        new AlgorithmComboboxLoader(algorithmBoxModel, pluginDir).load();
        if (algorithmBoxModel.getSize() > 0) {
            configureButton.setEnabled(true);
            searchButton.setEnabled(true);
            algorithmComboBox.setSelectedIndex(0);
        }
    }

    private void showHideGraph() {
        mapWrapper.paintGraph(null);
        if (paintGraphMenuItem.isSelected()) {
            mapWrapper.paintGraph(graph);
        }
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

        List<Path<?, ? extends Node, ? extends Link>> pathList = new ArrayList<>();
        for (SimplexResultEntry resultEntry : items) {
            pathList.add(resultEntry.getPath());
            list.add(resultEntry);
            log.log(Level.INFO, "highlighted: {0}", resultEntry.getPath().toString());
        }
        mapWrapper.paintPaths(pathList);

        simplexControl.setHighlight(list);
        mapWrapper.repaint();
    }

    private void startAndRunAlgorithm() {
        resultTableModel.setRowCount(0);
        simplexControl1D.setPoints(Collections.EMPTY_LIST);
        simplexControl2D.setPoints(Collections.EMPTY_LIST);
        simplexControl3D.setPoints(Collections.EMPTY_LIST);
        mapWrapper.paintPaths(Collections.EMPTY_LIST);
        mapWrapper.paintNodes(Collections.EMPTY_LIST);
        repaint();

        if (graph == null) {
            JOptionPane.showMessageDialog(this, "Please load graph first.");
            return;
        }

        if (nodeListPanel.getNodes().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Route including start and endpoint must be set.");
            return;
        }

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
        mapWrapper.paintNodes(Collections.EMPTY_LIST);
        if (statistics != null && visitedNodesItem.isSelected()) {
            mapWrapper.paintNodes(statistics.getVisitedNodes());
        }
        mapWrapper.repaint();
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
        currentAlgorithm.setNodes(nodeListPanel.getNodes());
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

        if (sourceFile.exists() && sourceFile.canRead()) {
            loadGraphWorker = new LoadGraphWorker(sourceFile, useTagWhitelist) {
                @Override
                protected void done() {
                    if (!isCancelled() && !Thread.interrupted()) {
                        try {
                            graph = get();
                            graphLoaded();
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(TrafficminingGUI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };
            busyLabel.setBusy(true);
            loadGraphWorker.execute();
        }
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

    private void openPBFWindow() {
        PbfImportFrame frame = new PbfImportFrame();
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
        resultTableModel = new de.lmu.ifi.dbs.trafficmining.ui.ReadOnlyTableModel();
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
        nodeListPanel = new de.lmu.ifi.dbs.trafficmining.ui.nodelist.NodeListPanel();
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
        mapWrapper = new de.lmu.ifi.dbs.trafficmining.ui.MapWrapper();
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
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

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
        waypointPanel.setLayout(new java.awt.BorderLayout());
        waypointPanel.add(nodeListPanel, java.awt.BorderLayout.CENTER);

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
        rightPanel.add(mapWrapper, java.awt.BorderLayout.CENTER);

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
        fileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuActionPerformed(evt);
            }
        });

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
        saveProperties();
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * user selected something in the algorithm list
     */
    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        startAndRunAlgorithm();
    }//GEN-LAST:event_searchButtonActionPerformed

    private void paintGraphMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paintGraphMenuItemActionPerformed
        showHideGraph();
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

private void importPbfMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPbfMenuItemActionPerformed
    openPBFWindow();
}//GEN-LAST:event_importPbfMenuItemActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed

    AboutDialog ad = new AboutDialog(this, true);
    ad.setLocationRelativeTo(null);
    ad.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        saveProperties();
    }//GEN-LAST:event_formWindowClosing

    private void fileMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fileMenuActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.DefaultComboBoxModel algorithmBoxModel;
    private javax.swing.JComboBox algorithmComboBox;
    private javax.swing.JCheckBoxMenuItem autoloadMenuItem;
    private org.jdesktop.swingx.JXBusyLabel busyLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTree clusterTree;
    private javax.swing.JButton configureButton;
    private javax.swing.JMenuItem importPbfMenuItem;
    private javax.swing.JPanel leftPanel;
    private de.lmu.ifi.dbs.trafficmining.ui.MapWrapper mapWrapper;
    private de.lmu.ifi.dbs.trafficmining.ui.nodelist.NodeListPanel nodeListPanel;
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
                    final TrafficminingGUI d = new TrafficminingGUI();
                    d.setVisible(true);
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            d.saveProperties();
                        }
                    });
                } catch (Throwable ex) {
                    Logger.getLogger(TrafficminingGUI.class.getName()).log(Level.SEVERE, null, ex);
                    System.exit(1);
                }
            }
        });
    }
}
