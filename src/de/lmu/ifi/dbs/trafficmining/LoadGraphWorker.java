package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.utils.XmlOsmGraphReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Class to asynchronously load nodes and ways into a graph representation.
 *
 * @author graf
 */
public class LoadGraphWorker extends SwingWorker<Graph, Void> {

    private static final String WHITELIST_KEY = "tags";
    private static final String WHITELIST_VALUE_SEPARATOR = ";";
    private static final Logger log = Logger.getLogger(LoadGraphWorker.class.getName());
    private final File osmXml;
    private final List<String> tagWhitelist;

    /**
     * Initializes the worker.
     *
     * A whitelist file can be handed over to the worker IF whitelisting should
     * occur. This whitelist file is a simple properties file with a key
     * <code>tags</code> followed by a ";" separated list of tag names.
     *
     * @param osmFile the source osm file
     * @param useTagWhitelist use tag whitelist specified at
     * TrafficminingProperties.TAG_WHITELIST_FILE
     */
    public LoadGraphWorker(File osmFile, boolean useTagWhitelist) {
        if (osmFile == null){
            throw new NullPointerException("osmFile must not be null");
        }
        if (!osmFile.exists() || !osmFile.canRead()) {
            throw new IllegalArgumentException("the file " + osmFile.getName() + " doesn't exist or is not readable.");
        }
        if (!osmFile.getName().toLowerCase().endsWith(".osm")) {
            log.warning("the file which is tried to be loaded does not end in '.osm' - this might indicate an error: " + osmFile.getName());
        }
        osmXml = osmFile;

        if (useTagWhitelist) {
            tagWhitelist = initTagWhitelist();
        } else {
            log.info("tag whitelist file either null, not found or not readable - ignoring");
            tagWhitelist = null;
        }
    }

    @Override
    protected Graph doInBackground() throws IOException, ParserConfigurationException, SAXException {
        Graph<Node<Link>, Link<Node>> graph = null;
        try {
            log.log(Level.FINE, "reading graph from {0}", osmXml.getName());
            long a = System.currentTimeMillis();

            XmlOsmGraphReader reader = new XmlOsmGraphReader(osmXml, tagWhitelist);
            reader.process();
            graph = reader.getGraph();

            long b = System.currentTimeMillis();
            log.log(Level.FINE, "successfully read graph in {0}ms", (b - a));
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            log.log(Level.SEVERE, "Exception occured while reading the graph", ex);
            return null;
        }

        // Loading the Graph might have caused quite some overhead and a lot of 
        // now obsolete objects. Thus we request a System.gc to cleanup at once.
        // Keep in mind, that we only ASK the JVM to do a cleanup. This is not a 
        // "force gc"! 
        System.gc();
        System.gc();
        System.gc();
        return graph;
    }

    private List<String> initTagWhitelist() {
        List<String> whitelist = new ArrayList<>();
        File whitelistFile = new File(TrafficminingProperties.TAG_WHITELIST_FILE);

        try (BufferedReader br = new BufferedReader(new FileReader(whitelistFile))) {
            log.log(Level.FINE, "Using whitelist file for tags: {0}", whitelistFile.getAbsolutePath());
            Properties prop = new Properties();
            prop.load(br);

            String tagList = prop.getProperty(WHITELIST_KEY);
            if (tagList != null) {
                String[] tags = tagList.split(WHITELIST_VALUE_SEPARATOR);
                log.log(Level.FINE, "{0} tags listed for whitelisted", tags.length);
                for (String s : tags) {
                    whitelist.add(s.toLowerCase().intern());
                }
            }
            return whitelist;
        } catch (IOException e) {
            log.log(Level.WARNING, "An error occured due parsing the whitelist file, using no tag filtering: ", e);
            return null;
        }
    }
}
