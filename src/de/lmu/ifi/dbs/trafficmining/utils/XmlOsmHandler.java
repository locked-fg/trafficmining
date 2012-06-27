package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Handler class that generates OSMNodes and OSMLinks from plain .osm files.
 *
 * As not all tags of nodes and links may be relevant for each use case there is the possibility to add a whitelist for
 * tag names.
 *
 * @author greil, graf
 */
class XmlOsmHandler extends DefaultHandler {

    private static final Logger log = Logger.getLogger(XmlOsmHandler.class.getName());
    private HashMap<Integer, Node<Link>> nodesMap = new HashMap<>();
    private List<Node<Link>> nodes = new ArrayList<>();
    private List<Link<Node>> links = new ArrayList<>();
    private List<String> tagWhitelist = null;
    private DefaultHandler handler = null;

    public XmlOsmHandler() {
    }

    /**
     * Sets the processor's whit list that defines all tags that are allowed to be imported. Setting null deaktivates
     * the whitelist checking. Setting an empty list equals to not importing any any attributes at all.
     *
     * @param tagWhitelist
     */
    public void setTagWhitelist(List<String> tagWhitelist) {
        this.tagWhitelist = tagWhitelist;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (Thread.interrupted()) {
            return;
        }
        if (handler != null) {
            handler.startElement(uri, localName, qName, attributes);
        }

        switch (qName) {
            case "node":
                handler = new NodeHandler(attributes);
                break;

            case "way":
                handler = new WayHandler(attributes);
                break;

            default:
//                log.log(Level.FINE, "ignoring element: {0}", qName);
                break;
        }
    }

    /**
     * Replaces double quotes by single quotes
     *
     * @param s
     * @return modified string
     */
    private String clean(String s) {
        s = s.replace("\"", "'");
        return s;
    }

    /**
     * Checks i this tag is allowed to be imported
     *
     * @param s the stag to be checked
     * @return true if the whitelist is null or if s is contained in the whitelist
     */
    private boolean allowTag(String s) {
        if (tagWhitelist == null) {
            return true;
        } else {
            return tagWhitelist.contains(s.toLowerCase());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (Thread.interrupted()) {
            return;
        }
        if (handler != null) {
            handler.endElement(uri, localName, qName);
        } else {
//            log.log(Level.FINE, "ignoring closing element {0}", qName);
        }
    }

    public List<Node<Link>> getListNodes() {
        return nodes;
    }

    public List<Link<Node>> getListLinks() {
        return links;
    }

    class NodeHandler extends DefaultHandler {

        private final Node currentNode;

        private NodeHandler(Attributes attributes) {
            String id = attributes.getValue("id").intern();
            String lat = attributes.getValue("lat");
            String lon = attributes.getValue("lon");
            currentNode = new Node(Integer.parseInt(id));
            currentNode.setLat(Double.parseDouble(lat));
            currentNode.setLon(Double.parseDouble(lon));
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName) {
                case "tag":
                    String k = attributes.getValue("k");
                    if (allowTag(k)) {
                        String v = clean(attributes.getValue("v"));
                        if (k.equalsIgnoreCase("height")) {
                            setHeight(v);
                        } else {
                            currentNode.setAttr(k, v);
                        }
                    }
                    break;

                default:
                    throw new IllegalStateException(qName);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            nodes.add(currentNode);
            nodesMap.put(currentNode.getName(), currentNode);
            if (!nodes.isEmpty() && nodes.size() % 100000 == 0) {
                log.log(Level.FINE, "nodes: {0}", nodes.size());
            }
            handler = null;
        }

        private void setHeight(String v) {
            double height;
            try {
                height = Double.parseDouble(v);
            } catch (NumberFormatException nfe) {
                log.log(Level.WARNING, "WARNING! height is parsed to Double.NaN ", nfe);
                height = Double.NaN;
            }
            currentNode.setHeight(height);

        }
    }

    class WayHandler extends DefaultHandler {

        private final int wayId;
        private final List<Integer> intermediateNodeIds = new ArrayList<>();
        private final HashMap<String, String> wayAttributes = new HashMap<>();
        private boolean highway = false;
        private boolean oneway = false;
        private boolean reverse = false;

        private WayHandler(Attributes attributes) {
            wayId = Integer.parseInt(attributes.getValue("id"));
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName) {
                case "nd":
                    Integer nodeID = Integer.parseInt(attributes.getValue("ref"));
                    intermediateNodeIds.add(nodeID);
                    break;
                case "tag":
                    String k = attributes.getValue("k").intern();
                    if (allowTag(k)) {
                        String v = clean(attributes.getValue("v").intern());
                        wayAttributes.put(k, v);
                    }
                    break;
                default:
                    log.log(Level.FINE, "ignoring element {0} in way", qName);
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
                case "way":
                    Node src = nodesMap.get(intermediateNodeIds.get(0));
                    Node dst = nodesMap.get(intermediateNodeIds.get(intermediateNodeIds.size() - 1));

                    isHighway();
                    detectOneway();

                    if (src != null && dst != null && highway) {
                        Link<Node> link = initLink(src, dst);
                        setNodes(link);
                        setAttributes(link);
                        setAscendDescend(link);

                        links.add(link);
                        if (!links.isEmpty() && links.size() % 10000 == 0) {
                            log.log(Level.FINE, "links: {0}", links.size());
                        }
                    }
                    handler = null;
                    break;
                default:
                    break;
            }
        }

        private void detectOneway() {
            String onewayValue = wayAttributes.get("oneway");
            if (onewayValue == null) {
                return;
            }
            onewayValue = onewayValue.toLowerCase();
            switch (onewayValue) {
                case "yes":
                case "true":
                case "1":
                    oneway = true;
                    break;
                case "-1":
                    oneway = true;
                    reverse = true;
                    break;
                case "no":
                case "false":
                case "0":
                    break;
                default:
                    log.log(Level.WARNING, "ignoring oneway value {0}", onewayValue);
                    break;
            }
        }

        private void isHighway() {
            highway = wayAttributes.containsKey("highway");
        }

        private Link<Node> initLink(Node src, Node dst) {
            Link<Node> link;
            if (!reverse) {
                link = new Link(src, dst, oneway);
            } else {
                link = new Link(dst, src, oneway);
            }
            link.setId(wayId);
            return link;
        }

        private void setAttributes(Link<Node> link) throws NumberFormatException {
            for (String k : wayAttributes.keySet()) {
                String v = wayAttributes.get(k);
                switch (k) {
                    case "ascend":
                        link.setAscend(Double.parseDouble(v));
                        break;
                    case "descend":
                        link.setDescend(Double.parseDouble(v));
                        break;
                    case "incline":
                        // Steigung/Gefälle
                        // remove all non digits (like "%")
                        v = v.replaceAll("[^\\d]", "");
                        link.setAttr(k, v);
                        break;
                    default:
                        link.setAttr(k, v);
                        break;
                }
            }
        }

        private void setAscendDescend(Link<Node> link) {
            if (link.getAscend() == 0 && link.getDescend() == 0
                    && link.getSource().getHeight() != link.getTarget().getHeight()) {
                double height = link.getTarget().getHeight() - link.getSource().getHeight();
                if (!Double.isNaN(height)) {
                    if (height < 0) {
                        link.setDescend(-height);
                    } else {
                        link.setAscend(height);
                    }
                }
            }
        }

        private void setNodes(Link<Node> link) {
            // add intermediate nodes to the link incl. start/end node
            if (intermediateNodeIds.isEmpty()) {
                return;
            }

            if (reverse) {
                Collections.reverse(intermediateNodeIds);
            }

            for (Integer id : intermediateNodeIds) {
                Node node = nodesMap.get(id);
                if (node != null) {
                    link.addNodes(node);
                }
            }
        }
    }
}
