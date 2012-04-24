package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
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
 * As not all tags of nodes and links may be relevant for each use case there is
 * the possibility to add a witelist for tag names.
 *
 * @author greil
 */
class XmlOsmHandler extends DefaultHandler {

    private static final Logger log = Logger.getLogger(XmlOsmGraphReader.class.getName());
    private HashMap<Integer, OSMNode<OSMLink>> hm_nodes = new HashMap<>();
    private List<OSMNode<OSMLink>> list_nodes = new ArrayList<>();
    private LinkedList<String> list_nodeIDatWay = null;
    private LinkedList<String[]> list_wayAttribs = null;
    private List<OSMLink<OSMNode>> list_links = new ArrayList<>();
    private OSMNode currentNode = null;
    private boolean open_node = false; // indicates that a <node> is currently processed
    private boolean open_way = false;// indicates that a <way> is currently processed
    private String way_ID = "";
    private List<String> tagWhitelist = null;

    public XmlOsmHandler() {
    }

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
                //NODE
                open_node = true;
                String id = attributes.getValue("id").intern();
                String lat = attributes.getValue("lat");
                String lon = attributes.getValue("lon");
                currentNode = new OSMNode(Integer.parseInt(id));
                currentNode.setLat(Double.parseDouble(lat));
                currentNode.setLon(Double.parseDouble(lon));
                break;
            case "way":
                open_way = true;
                String wayID = attributes.getValue("id");
                way_ID = wayID;
                list_nodeIDatWay = new LinkedList<>();
                list_wayAttribs = new LinkedList<>();
                break;
            case "nd":
                String nodeID = attributes.getValue("ref").intern();
                list_nodeIDatWay.add(nodeID);
                break;
            case "tag":
                String k = attributes.getValue("k").intern();
                if (allowTag(k)) {
                    String v = clean(attributes.getValue("v").intern());
                    if (open_way) {
                        list_wayAttribs.add(new String[]{k, v});
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

    private String clean(String s) {
        s = s.replace("\"", "'");
        return s;
    }

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
                OSMLink<OSMNode> link;
                open_way = false;
                OSMNode src = null;
                OSMNode dst = null;
                String first = list_nodeIDatWay.peekFirst().intern();
                String last = list_nodeIDatWay.peekLast().intern();
                if (first != null) {
                    src = hm_nodes.get(Integer.parseInt(first));
                }
                if (last != null) {
                    dst = hm_nodes.get(Integer.parseInt(last));
                }
                boolean highway = false;
                boolean oneway = false;
                boolean reverse = false;
                if (src != null && dst != null) {
                    // is it a one way street?
                    for (String[] pair : list_wayAttribs) {
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
                        link = new OSMLink(src, dst, oneway);
                    } else {
                        link = new OSMLink(dst, src, oneway);
                    }
                    link.setId(Integer.parseInt(way_ID));

                    // add intermediate nodes to the link incl. start/end node
                    while (!list_nodeIDatWay.isEmpty()) {
                        String nodeID = "";
                        if (!reverse) {
                            nodeID = list_nodeIDatWay.pollFirst().intern();
                        } else {
                            nodeID = list_nodeIDatWay.pollLast().intern();
                        }
                        if (nodeID != null) {
                            OSMNode worky = hm_nodes.get(Integer.parseInt(nodeID));
                            if (worky != null) {
                                link.addNodes(worky);
                            }
                        }
                    }

                    // add attributes
                    for (String[] pair : list_wayAttribs) {
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
                    list_links.add(link);
                    if (!list_links.isEmpty() && list_links.size() % 10000 == 0) {
                        log.log(Level.FINE, "links: {0}", list_links.size());
                    }
                }
                way_ID = null;
                list_nodeIDatWay = null;
                list_wayAttribs = null;
                break;
            case "node":
                open_node = false;
                list_nodes.add(currentNode);
                hm_nodes.put(currentNode.getName(), currentNode);
                currentNode = null;
                if (!list_nodes.isEmpty() && list_nodes.size() % 100000 == 0) {
                    log.log(Level.FINE, "nodes: {0}", list_nodes.size());
                }
                break;
        }
    }

    @Override
    public void endDocument() {
        log.log(Level.INFO, "total number: nodes: {0}, links: {1}", new Object[]{list_nodes.size(), list_links.size()});
    }

    public List<OSMNode<OSMLink>> getListNodes() {
        return list_nodes;
    }

    public List<OSMLink<OSMNode>> getListLinks() {
        return list_links;
    }
}
