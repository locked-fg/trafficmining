package de.lmu.ifi.dbs.paros.utils;

import de.lmu.ifi.dbs.paros.noOsm.CsvWeightedAdjacentListGraphReader;
import de.lmu.ifi.dbs.paros.noOsm.WeightedAdjacentListGraph;
import de.lmu.ifi.dbs.paros.graph.OSMGraph;
import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
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

    @Deprecated
    public static OSMGraph<OSMNode, OSMLink> readOsmGraphCsv(File nodes, File links) throws IOException {
        return new CsvOsmGraphReader().getGraph(nodes, links);
    }

    @Deprecated
    public static void writeOsmGraphCsv(OSMGraph g, File nodes, File ways) throws IOException {
        new CsvOsmGraphWriter().serializeTo(g, nodes, ways);
    }

    /**
     * create a {@link WeightedAdjacentListGraph} from CSV files
     * @param nodes
     * @param links
     * @return
     * @throws IOException
     */
    public static WeightedAdjacentListGraph readWeightedAdjacenceListGraphCsv(File nodes, File links) throws IOException {
        return new CsvWeightedAdjacentListGraphReader().getGraph(nodes, links);
    }

    /**
     * create a {@link WeightedAdjacentListGraph} from <b>OSM</b> CSV files
     * @param nodes
     * @param links
     * @return
     * @throws IOException
     */
    public static WeightedAdjacentListGraph readWeightedAdjacenceListGraphOsmCsv(File nodes, File links) throws IOException {
        return new CsvWeightedAdjacentListGraphReader().getGraphFromOsmData(nodes, links);
    }
}
