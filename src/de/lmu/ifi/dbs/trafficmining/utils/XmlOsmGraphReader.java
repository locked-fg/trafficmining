package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Class for reading OSM XML files
 * <p/>
 * @author graf @modified greil
 */
public class XmlOsmGraphReader<N extends OSMNode<L>, L extends OSMLink<N>> {

    private static final Logger log = Logger.getLogger(XmlOsmGraphReader.class.getName());
    private OSMGraph<N, L> graph = new OSMGraph<>();
    private XmlOsmHandler xmlHandler = new XmlOsmHandler();

    public XmlOsmGraphReader() {
    }

    private void parseOSMxml(File osmXML) {
        try {
            if (log.isLoggable(Level.INFO)) {
                long filesize = (osmXML.length() / 1024 / 1024);
                String out = filesize + " mb";
                if (filesize < 1) {
                    out = (osmXML.length() / 1024) + " kb";
                }
                log.log(Level.INFO, "parsing XML: {0}; filesize: {1}", new Object[]{osmXML.getAbsolutePath(), out});
            }
            if (Thread.interrupted()) {
                return;
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(osmXML, xmlHandler);
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(XmlOsmGraphReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initGraph() {
        if (Thread.interrupted()) {
            return;
        }

        List<N> nodes = xmlHandler.getListNodes();
        List<OSMLink<OSMNode>> links = xmlHandler.getListLinks();
        graph.addNodeList(nodes);
        graph.setLinkList(links);

    }

    public OSMGraph<N, L> getGraph(File osmXML) {
        parseOSMxml(osmXML);
        initGraph();
        graph.beautifyGraph();
        return graph;
    }
}
