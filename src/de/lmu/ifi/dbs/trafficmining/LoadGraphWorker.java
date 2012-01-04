package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import de.lmu.ifi.dbs.trafficmining.utils.XmlOsmGraphReader;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * Class o asynchronously load nodes and ways into a graph representation
 * <p/>
 * @author graf
 */
public class LoadGraphWorker extends SwingWorker<OSMGraph, Void> {

    private static final Logger log = Logger.getLogger(LoadGraphWorker.class.getName());
    private final File osmXml;

    public LoadGraphWorker(File f) {
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException(f.getName() + " doesn't exist or is not readable!");
        }
        if (!f.getName().toLowerCase().endsWith(".osm")) {
            throw new NullPointerException("*.osm file must not be null");
        }
        osmXml = f;
    }

    @Override
    protected OSMGraph doInBackground() throws Exception {
        log.log(Level.FINE, "reading graph from {0}", osmXml.getName());
        long a = System.currentTimeMillis();

        OSMGraph<OSMNode, OSMLink> graph = new XmlOsmGraphReader().getGraph(osmXml);

        long b = System.currentTimeMillis();
        log.log(Level.FINE, "read graph in {0}ms", (b - a));

        // Loading the Graph might have caused quite some overhead and a 
        // lot of now obsolete objects. Thus we request a System.gc to 
        // cleanup at once.
        // Keep in mind, that we only ASK the JVM to do a cleanup. this is not a "force gc"!
        System.gc();
        System.gc();
        System.gc();
        return graph;
    }
}
