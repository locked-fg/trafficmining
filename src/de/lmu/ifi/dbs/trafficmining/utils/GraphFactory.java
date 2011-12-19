package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import java.io.File;
import java.io.IOException;

/**
 * Class for reading/writing common formats of graphs
 * 
 * @author graf
 */
public class GraphFactory {

    private GraphFactory() {
    }

    public static OSMGraph<OSMNode, OSMLink> readOsmGraphXML(File osmXML) throws IOException {
        return new XmlOsmGraphReader().getGraph(osmXML);
    }

    public static void writeOsmGraphXML(OSMGraph g, File osmXML) throws IOException {
        throw (new UnsupportedOperationException("should not be needed!"));
    }
}
