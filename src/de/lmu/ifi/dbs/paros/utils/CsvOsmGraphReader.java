package de.lmu.ifi.dbs.paros.utils;

import de.lmu.ifi.dbs.paros.graph.OSMGraph;
import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Graph reader that reades the graph from a nodes and ways.txt
 * @author graf
 */
@Deprecated
public class CsvOsmGraphReader<N extends OSMNode<L>, L extends OSMLink<N>> {

    private static final Logger log = Logger.getLogger(CsvOsmGraphReader.class.getName());
    private OSMGraph<N, L> graph;

    public CsvOsmGraphReader() {
    }

    private void readNodes(File nodeFile) throws NumberFormatException,
            IOException {
        log.fine("reading nodes from " + nodeFile.getAbsolutePath());

        // read nodes
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(nodeFile), "UTF8"), 1 * 1024 * 1024);
            String line;
            while ((line = br.readLine()) != null && !Thread.interrupted()) {
                List<String[]> values = splitLine(line);
                OSMNode n = new OSMNode(Integer.parseInt(values.get(0)[1]));
                n.setLat(Double.parseDouble(values.get(1)[1]));
                n.setLon(Double.parseDouble(values.get(2)[1]));
                int i = 3;
                // height need not be set!
                if (values.size() >= i + 1 && values.get(3)[0].equals("height")) {
                    n.setHeight(Double.parseDouble(values.get(i++)[1]));
                }
                // remaining pairs must be attributes
                for (; i < values.size(); i++) {
                    n.setAttr(values.get(i)[0], values.get(i)[1]);
                }
                graph.addNode((N) n);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
        log.log(Level.FINE, "number of nodes: {0}", graph.getNodes().size());
    }

    private List<String[]> splitLine(String s) {
        List<String[]> list2 = new ArrayList<String[]>();
        final int QUOT = '"';
        final int SEP = ',';
        final int LENGTH = s.length();
        int indexA = 0;
        int indexB = 0;
        int stopchar = '-';

        while (indexA < LENGTH) {
            String[] pair = new String[2];
            // read key
            if (s.charAt(indexA) == QUOT) {
                stopchar = QUOT;
                indexA++;
            } else {
                stopchar = '=';
            }
            indexB = s.indexOf(stopchar, indexA);
            assert indexA >= 0 : "indexA must be >= 0 but was " + indexA + " in line: " + s;
            assert indexB >= 0 : "indexB must be >= 0 but was " + indexB + " in line: " + s;
            pair[0] = s.substring(indexA, indexB);
            indexA = indexB;
            if (stopchar == QUOT) {
                indexA++;
            }
            indexA++; // step over =

            // read value
            if (s.charAt(indexA) == QUOT) {
                stopchar = QUOT;
                indexA++;
            } else {
                stopchar = SEP;
            }
            indexB = s.indexOf(stopchar, indexA);
            indexB = (indexB == -1) ? LENGTH - 1 : indexB;
            pair[1] = s.substring(indexA, indexB);
            indexA = indexB;
            if (stopchar == QUOT) {
                indexA++;
            }
            indexA++; // Step over ,
            list2.add(pair);
        }
        return list2;
    }

    private void readWays(File linkFile) throws IOException,
            NumberFormatException {
        log.fine("reading ways from " + linkFile.getAbsolutePath());

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(linkFile), "UTF8"), 1 * 1024 * 1024);
            String line = null;
            while ((line = br.readLine()) != null && !Thread.interrupted()) {
                List<String[]> values = splitLine(line);
                OSMNode src = graph.getNode(Integer.parseInt(values.get(1)[1]));
                OSMNode dst = null;

                int lastNode = values.size();
                // find last "node" value. Search from the end of the list to the front
                while (--lastNode >= 0 && dst == null) {
                    if (values.get(lastNode)[0].equals("node")) {
                        dst = graph.getNode(Integer.parseInt(values.get(lastNode)[1]));
                        break;
                    }
                }
                assert lastNode >= 0 : line;
                if (src != null && dst != null) {
                    // is it a one way street?
                    boolean highway = false;
                    boolean oneway = false;
                    for (String[] pair : values) {
                        oneway |= pair[0].equals("oneway") && pair[1].equals("yes");
                        highway |= pair[0].equals("highway");
                    }
                    if (!highway) {
                        continue;
                    }

                    OSMLink<OSMNode> l = new OSMLink(src, dst, oneway);
                    assert values.get(0)[0].equals("id") : "id not found in line " + line;
                    l.setId(Integer.parseInt(values.get(0)[1]));

                    graph.getLinkList().add(l);
                    // add intermediate nodes to the link between 2 nodes
                    for (int i = 1; i <= lastNode; i++) {
                        OSMNode node = graph.getNode(Integer.parseInt(values.get(i)[1]));
                        if (node != null) {
                            l.addNodes(node);
                        }
                    }
                    // add attributes
                    for (int i = lastNode + 1; i < values.size(); i++) {
                        String[] pair = values.get(i);
                        /*if (pair[0].equals("distance")) {
                        l.setDistance(OSMUtils.dist(l));
                        } else*/
                        if (pair[0].equals("ascend")) {
                            l.setAscend(Double.parseDouble(pair[1]));
                        } else if (pair[0].equals("descend")) {
                            l.setDescend(Double.parseDouble(pair[1]));
                        } else if (pair[0].equals("incline")) { // Steigung/Gef�lle
                            // remove all non digits (like "%")
                            pair[1] = pair[1].replaceAll("[^\\d]", "");
                            l.setAttr(pair[0], pair[1]);
                            // TODO: check correctness!
//                    } else if (pair[0].equals("distance")) {
//                        try {
//                            Double dist = Double.parseDouble(pair[1]);
//                            if (dist != null && dist > 0) {
//                                l.setDistance(dist);
//                            }
//                        } catch (NumberFormatException ignore) {
//                        }
                        } else {
                            assert !pair[0].equals("node") : "node at pos " + i + " in line where it should not be? Line: " + line;
                            l.setAttr(pair[0], pair[1]);
                        }
                    }
                    if (l.getDistance() <= 0) {
                        l.setDistance(OSMUtils.dist(l));
                    }
                    if (l.getAscend() == 0 && l.getDescend() == 0 && l.getSource().getHeight() != l.getTarget().getHeight()) {
                        double height = l.getTarget().getHeight() - l.getSource().getHeight();
                        if (height < 0) {
                            l.setDescend(-height);
                        } else {
                            l.setAscend(height);
                        }
                    }
                    OSMUtils.setSpeed(graph, l);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
        log.fine("number of links: " + graph.getLinkList().size());
    }

    /**
     * Remove nodes without any links. This means also nodes that are used for
     * painting only.
     */
    private void cleanup() {
        List<N> nodesWithoutLinks = new ArrayList<N>();

        log.fine("removing nodes without links");
        nodesWithoutLinks.clear();
        List<N> list = new ArrayList<N>(graph.getNodes());
        for (N n : list) {
            if (Thread.interrupted()) {
                return;
            }
            if (n.getLinks().isEmpty()) {
                graph.removeNode(n);
                nodesWithoutLinks.add(n);
            }
        }
        log.fine("remaining nodes: " + graph.getNodes().size() + ", links: " + graph.getLinkList().size() + ". Removed " + nodesWithoutLinks.size() + " nodes");
    }

    public OSMGraph<N, L> getGraph(File nodeFile, File linkFile) throws
            IOException {
        graph = new OSMGraph<N, L>();
        readNodes(nodeFile);
        readWays(linkFile);
        cleanup();
        return graph;
    }
}
