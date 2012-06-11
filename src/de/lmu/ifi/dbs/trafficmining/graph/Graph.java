package de.lmu.ifi.dbs.trafficmining.graph;

import de.lmu.ifi.dbs.trafficmining.TrafficminingProperties;
import de.lmu.ifi.dbs.trafficmining.utils.GeoDistance;
import de.lmu.ifi.dbs.trafficmining.utils.GreatcircleDistance;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author graf
 * @param <N>
 * @param <L>
 */
public class Graph<N extends Node, L extends Link>  {

    private static final Logger log = Logger.getLogger(Graph.class.getName());
    private GeoDistance distance = new GreatcircleDistance();
    protected List<Link<Node>> linkList = new ArrayList<>();
    protected HashMap<String, Integer> speed = new HashMap<>();
    //do not split links at these attributes
    private final String[] blacklistForLinkSplitting = new String[]{
        "height",
        "name",
        "note"
    };
    private HashMap<Integer, N> nodes = new HashMap<>();

    public Graph() {
        speed.clear();
        setDefaultSpeeds();
        try {
            loadSpeedMap();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error loading speedmap. Defaults remain.", ex);
        }
    }

    public int getLinkCount() {
        return linkList.size();
    }

    public void beautifyGraph() {
        if (!Thread.interrupted()) {
            splitNodeWithAttribsToNewLinks();
        }
        if (!Thread.interrupted()) {
            connect();
        }
        if (!Thread.interrupted()) {
            cleanNodeList();
        }

    }

//    public Node getNode(Double lat, Double lon) {
//        Collection<N> nodes = this.getNodes();
//        for (N aktN : nodes) {
//            if (aktN.getLat() == lat && aktN.getLon() == lon) {
//                return aktN;
//            }
//        }
//        return null;
//    }
    /**
     * if a node has an additional attribute and is an intermediate node split
     * the link to two new links and set the node as start or end for a new link
     */
    private void splitNodeWithAttribsToNewLinks() {
        int s = linkList.size();
        log.log(Level.INFO, "creating new links for nodes with attribs. Links: {0}", s);
        List<Link<Node>> newLinkList = new ArrayList<>(s);
        int id_counter = -123;

        // list with all active links
        for (int o = 0; o < linkList.size() && !Thread.interrupted(); o++) {
            //specified link
            Link<Node> link_org = linkList.get(o);

            //list with all nodes per specified link
            List<Node> list_nodes = link_org.getNodes();

            //list with new nodes for new link
            List<Node> act_nodes = new ArrayList<>();
            boolean link_splitted = false;

            for (int l = 0; l < list_nodes.size(); l++) {
                Node n = list_nodes.get(l);

                if (l == 0) { // first node
                    act_nodes.add(n);
                } else if (l == list_nodes.size() - 1) {  // last node
                    if (link_splitted) {  // at least 1 node had attributes
                        act_nodes.add(n);
                        Link<Node> lw = newLinker(act_nodes, id_counter, link_org);
                        newLinkList.add(lw);
                        id_counter--;
                    } else { // no node had attribs, maintain the original unmodified link
                        act_nodes.clear();
                        link_org.setLength(distance.length(link_org));
                        newLinkList.add(link_org);
                    }
                } else {  // intermediate nodes
                    act_nodes.add(n);

                    boolean usefulAttribs = false;
                    Map<String, String> map_node_attr = n.getAttr();
                    if (map_node_attr.size() > 0) {
                        for (String attrib : map_node_attr.keySet()) {
                            for (String bl : blacklistForLinkSplitting) {
                                if (!attrib.equalsIgnoreCase(bl)) {
                                    usefulAttribs = true; // node has informational attribs except the blacklisted ones e.g. height (no need to split here)
                                }
                            }
                            if (usefulAttribs) {
                                break;
                            }
                        }
                    }
                    if (usefulAttribs) {  // split link
                        link_splitted = true;
                        Link<Node> lw = newLinker(act_nodes, id_counter, link_org);
                        newLinkList.add(lw);

                        id_counter--;
                        act_nodes.clear();
                        act_nodes.add(n);
                    }
                }
            }
        }
        linkList.clear();
        for (Link<Node> oSMLink : newLinkList) {
            linkList.add(oSMLink);
            OSMUtils.setSpeed(this, oSMLink);
        }
//        linkList = newLinkList;
//        for (OSMLink<OSMNode> oSMLink : newLinkList) {
//            System.out.println("oSMLink: "+oSMLink+" speed: "+oSMLink.getSpeed()+" | distance: "+oSMLink.getDistance()+" | oneway: "+oSMLink.isOneway());
//            for (OSMNode oSMNode : oSMLink.getNodes()) {
//                System.out.println(oSMNode.getName()+" : "+oSMNode);
//            }
//        }
        log.log(Level.INFO, "Done. Added {0} links. Links now: {1}", new Object[]{linkList.size() - s, linkList.size()});
    }

    private Link<Node> newLinker(List<Node> nodes, int id, Link<Node> link_org) {
        if (nodes.size() < 2) {
            log.log(Level.SEVERE, "{0}.newLinker: nodes.size() < 2: {1}", new Object[]{this.getClass().getName(), nodes.size()});
            throw new IllegalStateException("linking less than 2 nodes does not work");
        }

        Link<Node> result = new Link(nodes.get(0), nodes.get(nodes.size() - 1), link_org.isOneway());
        for (Node oSMNode : nodes) {
            oSMNode.removeLink(link_org);  // reset the node<>link memory
            result.addNodes(oSMNode);  // and readd the nodes to the link
        }

        result.setId(id);
        result.setAscend(link_org.getAscend());
        result.setDescend(link_org.getDescend());
        for (Map.Entry<String, String> entry : link_org.getAttr().entrySet()) {
            result.setAttr(entry.getKey(), entry.getValue());
        }
        result.setLength(distance.length(result));
        return result;
    }

    private void cleanNodeList() {
        int counter = 0;
        log.log(Level.INFO, "removing nodes without links. Nodes: {0}", getNodes().size());
        List<N> list = new ArrayList<>(getNodes());
        for (N n : list) {
            if (Thread.interrupted()) {
                return;
            }
            if (n.getLinks().isEmpty()) {
                removeNode(n);
                counter++;
            }
        }
        log.log(Level.INFO, "remaining nodes: {0}, links: {1}. Removed {2} nodes.", new Object[]{getNodes().size(), getLinkList().size(), counter});
    }

    /**
     * @fixme this should be done MUCH more intelligent
     *
     * AC and BD are links, but not AB, BC. So split AC into AB,BC.
     *
     * A--B--C | D
     */
    private void connect() {
        log.log(Level.INFO, "connecting ways for routing");
        int linkCountA = linkList.size();
        List<Node> nodes = new ArrayList<>(1000);
        for (Node n : getNodes()) {
            if (n.getLinks().size() > 0) {
                nodes.add(n);
            }
        }

        for (int i = 0; i < linkList.size() && !Thread.interrupted(); i++) {
            if (i != 0 && i % 10000 == 0) {
                log.log(Level.FINE, "processed {0} / {1} links", new Object[]{i, linkList.size()});
            }

            int removes = 0;
            int adds = 0;
            Link<Node> link = linkList.get(i);
            List<Node> linkNodes = link.getNodes();
            for (int j = 0; link != null && j < linkNodes.size() && !Thread.interrupted(); j++) {
                Node innerNode = linkNodes.get(j);
                List<Link<Node>> links = innerNode.getLinks();
                if (links.size() > 0 && !links.contains(link)) {
                    linkList.remove(i);
                    removes++;
                    List<Link<Node>> newLinks = OSMUtils.split(link, innerNode, distance);
                    for (Link<Node> aLink : newLinks) {
                        linkList.add(i, aLink);
                        adds++;
                    }
                    link = null;
                    linkNodes = null;
                    i--;
                }
            }
        }
        log.log(Level.INFO, "added {0} links.", (linkList.size() - linkCountA));
    }

    /**
     * Returns an instance of the link list. Keep in mind that changing the list
     * DOES NOT AFFECT the graph directly if you do not remove/add the link from
     * the according nodes!
     *
     * @return list of links
     */
    public List<Link<Node>> getLinkList() {
        return linkList;
    }

    public void setLinkList(List<Link<Node>> linkList) {
        this.linkList = linkList;
    }

    public HashMap<String, Integer> getSpeedMap() {
        return speed;
    }

    private void setDefaultSpeeds() {
        // http://wiki.openstreetmap.org/wiki/DE:Map_Features
        speed.put("default", 50); // myown definition
        //
        speed.put("DE:zone30", 30);
        speed.put("motorway", 130);
        speed.put("motorway_link", 60);
        speed.put("trunk", 100);
        speed.put("trunk_link", 60);
        speed.put("primary", 100);
        speed.put("primary_link", 60);
        speed.put("secondary", 100);
        speed.put("secondary_link", 60);
        speed.put("tertiary", 100);
        speed.put("tertiary_link", 100);
        speed.put("unclassified", 50); // meist kleiner als tertiary
        speed.put("road", 50);
        speed.put("residential", 50);
        speed.put("living_street", 6); // spielstraße 4-7 km/h
        speed.put("service", 30);
        speed.put("track", 30);
        speed.put("pedestrian", 10);
        speed.put("raceway", 250);
        speed.put("services", 60);
        speed.put("bus_guideway", 60);
        speed.put("construction", 3);
        // Paths
        speed.put("path", 4);
        speed.put("cycleway", 15);
        speed.put("footway", 5);
        speed.put("bridleway", 5);
        speed.put("byway", 15);
        speed.put("steps", 3);
    }

    private void loadSpeedMap() throws IOException {
        File speedFile = new File(TrafficminingProperties.SPEED_SETTINGS_FILE);
        Properties prop = new Properties();
        prop.load(new BufferedReader(new FileReader(speedFile)));
        log.log(Level.INFO, "Using speeds config: {0}", speedFile.getAbsolutePath());
        log.log(Level.FINE, "Speeds found: {0}", prop.keySet().size());

        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            Integer speed_int = Integer.parseInt((String) entry.getValue());
            speed.put((String) entry.getKey(), speed_int);
            log.log(Level.FINE, "{0} -> {1}", new Object[]{entry.getKey(), speed_int});
        }
    }

    public void addNode(N node) {
        nodes.put(node.getName(), node);
    }

    public void addNodeList(List<N> nodeList) {
        for (N n : nodeList) {
            nodes.put(n.getName(), n);
        }
    }

    public void removeNode(N node) {
        nodes.remove(node.getName());
    }

    public void removeNode(int id) {
        nodes.remove(id);
    }

    public N getNode(int name) {
        return nodes.get(name);
    }

    public Collection<N> getNodes() {
        return nodes.values();
    }

    public int nodeCount() {
        return nodes.size();
    }
}
