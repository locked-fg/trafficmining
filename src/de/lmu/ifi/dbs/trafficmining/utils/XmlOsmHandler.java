package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import java.util.ArrayList;
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

    private static final Logger log = Logger.getLogger(XmlOsmGraphReader.class.getName());
    private HashMap<Integer, Node<Link>> nodesMap = new HashMap<>();
    private List<Node<Link>> nodes = new ArrayList<>();
    private LinkedList<Integer> intermediateNodeIds = null;
    private List<String[]> wayAttributes = null;
    private List<Link<Node>> links = new ArrayList<>();
    private Node currentNode = null;
    private Integer wayId = null;
    private List<String> tagWhitelist = null;
    private boolean open_node = false; // indicates that a <node> is currently processed
    private boolean open_way = false;// indicates that a <way> is currently processed

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
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (Thread.interrupted()) {
            return;
        }
        switch (qName) {
            case "node":
                open_node = true;
                String id = attributes.getValue("id").intern();
                String lat = attributes.getValue("lat");
                String lon = attributes.getValue("lon");
                currentNode = new Node(Integer.parseInt(id));
                currentNode.setLat(Double.parseDouble(lat));
                currentNode.setLon(Double.parseDouble(lon));
                break;
            case "way":
                open_way = true;
                wayId = Integer.parseInt(attributes.getValue("id"));
                intermediateNodeIds = new LinkedList<>();
                wayAttributes = new LinkedList<>();
                break;
            case "nd":
                Integer nodeID = Integer.parseInt(attributes.getValue("ref").intern());
                intermediateNodeIds.add(nodeID);
                break;
            case "tag":
                String k = attributes.getValue("k").intern();
                if (allowTag(k)) {
                    String v = clean(attributes.getValue("v").intern());
                    if (open_way) {
                        wayAttributes.add(new String[]{k, v});
                    } else if (open_node) {
                        if (k.equalsIgnoreCase("height")) {
                            double height = 0;
                            try {
                                height = Double.parseDouble(v);
                            } catch (NumberFormatException nfe) {
                                log.warning("WARNING! height is parsed to Double.NaN");
                                height = Double.NaN;
                            }
                            currentNode.setHeight(height);
                        } else {
                            currentNode.setAttr(k, v);
                        }
                    }
                }
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
    public void endElement(String uri, String localName, String qName) {
        if (Thread.interrupted()) {
            return;
        }
        switch (qName) {
            case "way":
                Link<Node> link;
                open_way = false;
                Node src = null;
                Node dst = null;
                Integer first = intermediateNodeIds.peekFirst();
                Integer last = intermediateNodeIds.peekLast();
                if (first != null) {
                    src = nodesMap.get(first);
                }
                if (last != null) {
                    dst = nodesMap.get(last);
                }
                boolean highway = false;
                boolean oneway = false;
                boolean reverse = false;
                if (src != null && dst != null) {
                    // is it a one way street?
                    for (String[] pair : wayAttributes) {
                        if (pair[0].equals("oneway")) {
                            switch (pair[1]) {
                                case "yes":
                                case "true":
                                case "1":
                                    oneway = true;
                                    break;
                                case "-1":
                                    // oneway:-1
                                    oneway = true;
                                    reverse = true;
                                    break;
                            }
                        }
                        if (pair[0].equals("highway")) {
                            highway = true;
                        }
                    }
                }
                if (highway) {
                    if (!reverse) {
                        link = new Link(src, dst, oneway);
                    } else {
                        link = new Link(dst, src, oneway);
                    }
                    link.setId(wayId);

                    // add intermediate nodes to the link incl. start/end node
                    while (!intermediateNodeIds.isEmpty()) {
                        Integer nodeId;
                        if (!reverse) {
                            nodeId = intermediateNodeIds.pollFirst();
                        } else {
                            nodeId = intermediateNodeIds.pollLast();
                        }
                        if (nodeId != null) {
                            Node worky = nodesMap.get(nodeId);
                            if (worky != null) {
                                link.addNodes(worky);
                            }
                        }
                    }

                    // add attributes
                    for (String[] pair : wayAttributes) {
                        switch (pair[0]) {
                            case "ascend":
                                link.setAscend(Double.parseDouble(pair[1]));
                                break;
                            case "descend":
                                link.setDescend(Double.parseDouble(pair[1]));
                                break;
                            case "incline":
                                // Steigung/Gefälle
                                // remove all non digits (like "%")
                                pair[1] = pair[1].replaceAll("[^\\d]", "");
                                link.setAttr(pair[0], pair[1]);
                                break;
                            default:
                                link.setAttr(pair[0], pair[1]);
                                break;
                        }
                    }
                    if (link.getAscend() == 0 && link.getDescend() == 0 && link.getSource().getHeight() != link.getTarget().getHeight()) {
                        double height = link.getTarget().getHeight() - link.getSource().getHeight();
                        if (!Double.isNaN(height)) {
                            if (height < 0) {
                                link.setDescend(-height);
                            } else {
                                link.setAscend(height);
                            }
                        }
                    }
                    links.add(link);
                    if (!links.isEmpty() && links.size() % 10000 == 0) {
                        log.log(Level.FINE, "links: {0}", links.size());
                    }
                }
                wayId = null;
                intermediateNodeIds = null;
                wayAttributes = null;
                break;
            case "node":
                open_node = false;
                nodes.add(currentNode);
                nodesMap.put(currentNode.getName(), currentNode);
                currentNode = null;
                if (!nodes.isEmpty() && nodes.size() % 100000 == 0) {
                    log.log(Level.FINE, "nodes: {0}", nodes.size());
                }
                break;
        }
    }

    @Override
    public void endDocument() {
        log.log(Level.INFO, "total number: nodes: {0}, links: {1}", new Object[]{nodes.size(), links.size()});
    }

    public List<Node<Link>> getListNodes() {
        return nodes;
    }

    public List<Link<Node>> getListLinks() {
        return links;
    }
}
