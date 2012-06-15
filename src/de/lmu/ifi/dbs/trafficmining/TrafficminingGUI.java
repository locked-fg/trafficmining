package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.result.*;
import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl;
import de.lmu.ifi.dbs.trafficmining.ui.AboutDialog;
import de.lmu.ifi.dbs.trafficmining.ui.EnableTileserverAction;
import de.lmu.ifi.dbs.trafficmining.ui.PbfImportFrame;
import de.lmu.ifi.dbs.trafficmining.ui.StatisticsFrame;
import de.lmu.ifi.dbs.trafficmining.ui.TileServerFactory;
import de.lmu.ifi.dbs.trafficmining.ui.algorithm.AlgorithmPanel;
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
import javax.swing.filechooser.FileFilter;
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
    private JFrame parentFrame;

    public TrafficminingGUI() throws IOException {
        log.info("start");
        parentFrame = this;
        initComponents();

        this.properties = new TrafficminingProperties();
        this.node2mapConnector = new NodeList2MapConnector(nodeListPanel, mapWrapper);
        setLocationRelativeTo(null);

        initResultBindings();

        restoreLastMapPosition();
        maybeLoadRecentGraph();

        resultPanel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                reloadStatisticsData();
                highlightResult();
            }
        });

        TileServerFactory.get();
        initTileServerMenu();

        // configure algorithmpanel
        algorithmPanel.setPluginDir(properties.getFile(TrafficminingProperties.plugin_dir));
        algorithmPanel.addButtonObserver(new AlgorithmPanelObserver());
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
//        simplexControl1D.addMouseListener(new SimplexHighlighter(resultTable));
//        simplexControl2D.addMouseListener(new SimplexHighlighter(resultTable));
//        simplexControl3D.addMouseListener(new SimplexHighlighter(resultTable));
    }

    private void restoreLastMapPosition() {
        try {
            Double lat = properties.getDouble(TrafficminingProperties.map_last_center_latitude);
            Double lon = properties.getDouble(TrafficminingProperties.map_last_center_longitude);
            if (lat != null && lon != null && lat != 1 && lon != 1) {
                mapWrapper.setCenterPosition(new GeoPosition(lat, lon));
            }
        } catch (NumberFormatException nfe) {
            log.log(Level.INFO, "map location cannot be restored", nfe);
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
        algorithmPanel.setBusy(false);
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
        results.clear();
        resultPanel.setResult(result);

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

        if (resultPanel.getSelectedRowCount() > 0) {
            for (int rowId : resultPanel.getSelectedRows()) {
                Integer resultId = (Integer) resultPanel.getValueAt(rowId);
                SimplexResultEntry entry = results.get(resultId);
                if (entry != null) {
                    Map<String, String> mapIntern = statistics.getPath(entry.getPath());
                    statisticsFrame.addPathData(mapIntern, resultId);
                }
            }
        }
    }

    private void showHideGraph() {
        mapWrapper.paintGraph(null);
        if (paintGraphMenuItem.isSelected()) {
            mapWrapper.paintGraph(graph);
        }
    }

    private void highlightResult() {
        int[] rowIDs = resultPanel.getSelectedRows();
        if (rowIDs.length == 0 || results.isEmpty() || resultPanel.getRowCount() == 0) {
            return;
        }

        List<SimplexResultEntry> items = new ArrayList<>();
        for (int rowID : rowIDs) {
            rowID = resultPanel.convertRowIndexToModel(rowID);
            Integer id = (Integer) resultPanel.getValueAt(rowID);
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
        resultPanel.setResult(null);
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
            resultPanel.setResult(null);
            calculator.cancel(true);
            calculator = null;
        }

        algorithmPanel.setBusy(false);

        currentAlgorithm.setGraph(graph);
        currentAlgorithm.setNodes(nodeListPanel.getNodes());
        calculator = new AlgorithmWorker(currentAlgorithm);
        calculator.addPropertyChangeListener(new AlgorithmWorkerListener(this));
        algorithmPanel.setBusy(true);
        calculator.execute();
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


        // FIXME
        resultPanel.setResult(result);
        Map<Path, double[]> resultsMap = result.getResults();
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
            pointSourceList.add(resEntry);
        }

        SimplexControl simplexControl = resultToSimplexControl.get(result.getClass());
        simplexControl.setAttributNames(result.getAttributes());
        simplexControl.setPoints(pointSourceList);

        if (statisticsFrame != null && statisticsFrame.isVisible()) {
            showStatisticsFrame();
        }
        showHideVisitedNodes();
        highlightResult();
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
            algorithmPanel.setBusy(true);
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
                algorithmPanel.setBusy(false);
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

        javax.swing.JSplitPane horizontalSplit = new javax.swing.JSplitPane();
        verticalSplitPane = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        waypointPanel = new javax.swing.JPanel();
        nodeListPanel = new de.lmu.ifi.dbs.trafficmining.ui.nodelist.NodeListPanel();
        javax.swing.JButton showStatisticsButton = new javax.swing.JButton();
        algorithmPanel = new de.lmu.ifi.dbs.trafficmining.ui.algorithm.AlgorithmPanel();
        resultPanel = new de.lmu.ifi.dbs.trafficmining.ui.result.ResultPanel();
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

        algorithmPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithm"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        leftPanel.add(algorithmPanel, gridBagConstraints);

        resultPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Result Routes"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.75;
        leftPanel.add(resultPanel, gridBagConstraints);

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

    private void paintGraphMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paintGraphMenuItemActionPerformed
        showHideGraph();
    }//GEN-LAST:event_paintGraphMenuItemActionPerformed

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
    private de.lmu.ifi.dbs.trafficmining.ui.algorithm.AlgorithmPanel algorithmPanel;
    private javax.swing.JCheckBoxMenuItem autoloadMenuItem;
    private javax.swing.JMenuItem importPbfMenuItem;
    private javax.swing.JPanel leftPanel;
    private de.lmu.ifi.dbs.trafficmining.ui.MapWrapper mapWrapper;
    private de.lmu.ifi.dbs.trafficmining.ui.nodelist.NodeListPanel nodeListPanel;
    private javax.swing.JCheckBoxMenuItem paintGraphMenuItem;
    private de.lmu.ifi.dbs.trafficmining.ui.result.ResultPanel resultPanel;
    private javax.swing.JPanel rightPanel;
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

    private class AlgorithmPanelObserver implements Observer {

        @Override
        public void update(Observable o, Object command) {
            try {
                if (command.equals(AlgorithmPanel.EVT_CONFIG)) {
                    currentAlgorithm = algorithmPanel.ensureInitAlgorithm(currentAlgorithm);
                    algorithmPanel.configureAlgorithm(currentAlgorithm, parentFrame);
                }
                if (command.equals(AlgorithmPanel.EVT_EXECUTE)) {
                    currentAlgorithm = algorithmPanel.ensureInitAlgorithm(currentAlgorithm);
                    startAndRunAlgorithm();
                }
                if (command.equals(AlgorithmPanel.EVT_CANCEL)) {
                    calculator.cancel(true);
                }
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(TrafficminingGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
