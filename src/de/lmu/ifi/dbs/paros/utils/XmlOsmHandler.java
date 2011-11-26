/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package de.lmu.ifi.dbs.paros.utils;

import de.lmu.ifi.dbs.paros.PAROS;
import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.xml.sax.Attributes;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author greil
 */
public class XmlOsmHandler<N extends OSMNode<L>, L extends OSMLink<N>> extends DefaultHandler {

    private static final Logger log = Logger.getLogger(XmlOsmGraphReader.class.getName());
    private HashMap<Integer, OSMNode> hm_nodes = new HashMap<>();
    private List<OSMNode> list_nodes = new ArrayList<>();
    private LinkedList<String> list_nodeIDatWay = null;
    private LinkedList<String[]> list_wayAttribs = null;
    private List<OSMLink<OSMNode>> list_links = new ArrayList<>();
    private OSMNode n = null;
    private boolean open_node = false; // indicates that a <node> is currently processed
    private boolean open_way = false;// indicates that a <way> is currently processed
    private String way_ID = "";
    private List<String> whitelist = null;
    private boolean wl_activated = PAROS.getWhitelistStatus();

    public XmlOsmHandler() {
        if (wl_activated) {
            Properties prop = new Properties();
            try {
//                File f = new File("blacklist-tags.properties");
                File f = new File("whitelist-tags.properties");
                prop.load(new BufferedReader(new FileReader(f)));
                String[] tags = prop.getProperty("tags").split(";");
                whitelist = new ArrayList<String>();
                log.log(Level.INFO, "Using whitelist for tags: {0}", f.getAbsolutePath());
                log.log(Level.FINE, "Tags whitelisted: {0}", tags.length);
                for (String s : tags) {
                    String tag = s.toLowerCase();
                    whitelist.add(tag);
                    log.log(Level.FINE, "tag: {0}", tag);
                }
//                log.info("Using blacklist for tags: " + f.getAbsolutePath());
//                log.fine("Tags blacklisted: " + blacklist.size());


            } catch (Exception e) {
                e.printStackTrace();
                wl_activated = false;
                log.info("An error occured due parsing whitelist, using no tag filtering");
            }
        }
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if (qName.equals("node")) { //NODE
                open_node = true;
                String id = attributes.getValue("id");
                String lat = attributes.getValue("lat");
                String lon = attributes.getValue("lon");
                n = new OSMNode(Integer.parseInt(id));
                n.setLat(Double.parseDouble(lat));
                n.setLon(Double.parseDouble(lon));
            } else if (qName.equals("way")) { //WAY
                open_way = true;
                String wayID = attributes.getValue("id");
                way_ID = wayID;
                list_nodeIDatWay = new LinkedList<String>();
                list_wayAttribs = new LinkedList<String[]>();
            } else if (qName.equals("nd")) {  //NODE AT WAY
                String nodeID = attributes.getValue("ref");
                list_nodeIDatWay.add(nodeID);
            } else if (qName.equals("tag")) {  //TAG
                String k = attributes.getValue("k");
                if (allowTag(k)) {
                    String v = clean(attributes.getValue("v"));
                    if (open_way) {
                        list_wayAttribs.add(new String[]{k, v});
                    } else if (open_node) {
                        if (k.equalsIgnoreCase("height")) {
                            try {
                                double height = Double.parseDouble(v);
                                n.setHeight(height);
                            } catch (NumberFormatException nfe) {
                            }
                        } else {
                            n.setAttr(k, v);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String clean(String s) {
        s = s.replace("\"", "'");
        return s;
    }

    private boolean allowTag(String s) {
        if (!wl_activated) {
            return true;
        } else {
            if (whitelist.contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            switch (qName) {
                case "way":
                    OSMLink<OSMNode> link = null;
                    open_way = false;
                    OSMNode src = null;
                    OSMNode dst = null;
                    String first = list_nodeIDatWay.peekFirst();
                    String last = list_nodeIDatWay.peekLast();
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
                            OSMNode worky = null;
                            String nodeID = "";
                            if (!reverse) {
                                nodeID = list_nodeIDatWay.pollFirst();
                            } else {
                                nodeID = list_nodeIDatWay.pollLast();
                            }
                            if (nodeID != null) {
                                worky = hm_nodes.get(Integer.parseInt(nodeID));
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
                        //FIXME: not needed here.
                        //will be done again at graph.beautifyGraph();
    //                        link.setDistance(OSMUtils.dist(link));
                        if (link.getAscend() == 0 && link.getDescend() == 0 && link.getSource().getHeight() != link.getTarget().getHeight()) {
                            double height = link.getTarget().getHeight() - link.getSource().getHeight();
                            if (height < 0) {
                                link.setDescend(-height);
                            } else {
                                link.setAscend(height);
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
                    list_nodes.add(n);
                    hm_nodes.put(n.getName(), n);
                    n = null;
                    if (!list_nodes.isEmpty() && list_nodes.size() % 100000 == 0) {
                        log.log(Level.FINE, "nodes: {0}", list_nodes.size());
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void endDocument() {
        log.log(Level.INFO, "total number: nodes: {0}, links: {1}", new Object[]{list_nodes.size(), list_links.size()});
    }

    public List<OSMNode> getListNodes() {
        return list_nodes;
    }

    public List<OSMLink<OSMNode>> getListLinks() {
        return list_links;
    }

//    public void setBlackList(List<String> bl) {
//        whitelist = bl;
//        wl_activated = true;
//    }
}
