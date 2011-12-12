package de.lmu.ifi.dbs.paros;

import de.lmu.ifi.dbs.paros.graph.OSMGraph;
import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import de.lmu.ifi.dbs.paros.utils.GraphFactory;
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
//    private final File[] files;
    private final File file;
    private File osmXml = null;

    public LoadGraphWorker(File file) {
//        this.files = file;
        this.file = file;
    }

    public void init() {
//        for (File f : files) {
        File f = this.file;
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException(f.getName() + " doesn't exist or is not readable!");
        }
        if (f.getName().toLowerCase().endsWith(".osm")) {
            osmXml = f;
        }
//        }
        if (osmXml == null) {
            throw new NullPointerException("*.osm file must not be null");
        }

    }

    @Override
    protected OSMGraph doInBackground() throws Exception {
        init();
        OSMGraph<OSMNode, OSMLink> graph = null;
        try {
            log.fine("reading graph from " + osmXml.getName());
            long a = System.currentTimeMillis();
            graph = GraphFactory.readOsmGraphXML(osmXml);
            Thread.sleep(1);
            long b = System.currentTimeMillis();
            log.fine("read graph in " + (b - a) + "ms");

            // Loading the Graph might have caused quite some overhead and a 
            // lot of now obsolete objects. Thus we request a System.gc to 
            // cleanup at once.
            // Keep in mind, that we only ASK the JVM to do a cleanup. this is not a "force gc"!
            System.gc();
            System.gc();
            System.gc();
        } catch (InterruptedException ex) {
        } catch (Exception e) {
            log.severe("could not read graph!");
        }
        return graph;
    }
}
