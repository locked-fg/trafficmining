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
 * Class for reading OSM XML files into a Graph representation
 *
 * @author graf
 * @author greil
 */
public class XmlOsmGraphReader {

    private static final Logger log = Logger.getLogger(XmlOsmGraphReader.class.getName());
    private final File osmXML;
    private final List<String> tagWhitelist;
    private OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph;
    private XmlOsmHandler xmlHandler;

    public XmlOsmGraphReader(File osmXML, List<String> tagWhitelist) {
        this.osmXML = osmXML;
        this.tagWhitelist = tagWhitelist;
    }

    public void process() throws IOException, ParserConfigurationException,
            SAXException {

        if (Thread.interrupted()) {
            return;
        }

        parseXml(osmXML);

        if (Thread.interrupted()) {
            return;
        }

        graph = new OSMGraph<>();
        graph.addNodeList(xmlHandler.getListNodes());
        graph.setLinkList(xmlHandler.getListLinks());

        graph.beautifyGraph();
    }

    private void parseXml(File osmXML) throws ParserConfigurationException,
            SAXException, IOException {
        xmlHandler = new XmlOsmHandler();
        xmlHandler.setTagWhitelist(tagWhitelist);

        log.log(Level.INFO, "parsing XML: {0}; filesize: {1}kb", new Object[]{osmXML.getAbsolutePath(), osmXML.length() / 1024});

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);

        SAXParser parser = factory.newSAXParser();
        parser.parse(osmXML, xmlHandler);
    }

    public OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> getGraph() {
        return graph;
    }
}
