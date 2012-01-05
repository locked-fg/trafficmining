package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
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
public class LoadGraphWorker extends SwingWorker<OSMGraph, Void> {

    private static final String WHITELIST_KEY = "tags";
    private static final String WHITELIST_VALUE_SEPARATOR = ";";
    private static final Logger log = Logger.getLogger(LoadGraphWorker.class.getName());
    private final File whitelistFile;
    private final File osmXml;

    /**
     * Initialize the worker.
     *
     * A whitelist file can be handed over to the worker IF whitelisting should
     * occur. This whitelist file is a simple properties file with a key
     * <code>tags</code> followed by a ";" separated list of tag names.
     *
     * @param osmFile the source osm file
     * @param whitelistProperties properties file containing whitelisted node
     * attributes or null
     */
    public LoadGraphWorker(File osmFile, File whitelistProperties) {
        if (!osmFile.exists() || !osmFile.canRead()) {
            throw new IllegalArgumentException(osmFile.getName() + " doesn't exist or is not readable!");
        }
        if (!osmFile.getName().toLowerCase().endsWith(".osm")) {
            throw new NullPointerException("file must be an osm file (*.osm)");
        }
        osmXml = osmFile;

        // probably set the tagwhitelist
        if (whitelistProperties != null && whitelistProperties.exists() && whitelistProperties.canRead()) {
            this.whitelistFile = whitelistProperties;
        } else {
            log.info("tag whitelist file either null, not found or not readable - ignoring");
            this.whitelistFile = null;
        }
    }

    @Override
    protected OSMGraph doInBackground() {
        OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph = null;
        try {
            log.log(Level.FINE, "reading graph from {0}", osmXml.getName());
            long a = System.currentTimeMillis();

            List<String> tagWhitelist = initTagWhitelist();

            XmlOsmGraphReader reader = new XmlOsmGraphReader(osmXml, tagWhitelist);
            reader.process();
            graph = reader.getGraph();

            long b = System.currentTimeMillis();
            log.log(Level.FINE, "successfully read graph in {0}ms", (b - a));
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(LoadGraphWorker.class.getName()).log(Level.SEVERE, "Exception occured while reading the graph", ex);
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
        List<String> tagWhitelist = new ArrayList<>();

        if (whitelistFile != null) {
            try {
                log.log(Level.FINE, "Using whitelist file for tags: {0}", whitelistFile.getAbsolutePath());
                Properties prop = new Properties();
                prop.load(new BufferedReader(new FileReader(whitelistFile)));

                String tagList = prop.getProperty(WHITELIST_KEY);
                if (tagList != null) {
                    String[] tags = tagList.split(WHITELIST_VALUE_SEPARATOR);
                    log.log(Level.FINE, "{0} tags listed for whitelisted", tags.length);
                    for (String s : tags) {
                        tagWhitelist.add(s.toLowerCase().intern());
                    }
                }
            } catch (IOException e) {
                log.warning("An error occured due parsing the whitelist file, using no tag filtering");
                return null;
            }
        }
        return tagWhitelist;
    }
}
