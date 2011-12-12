package de.lmu.ifi.dbs.paros.utils;

import de.lmu.ifi.dbs.paros.graph.OSMGraph;
import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Writes an OSMGraph into 2 separate files
 * @author graf
 */
class CsvOsmGraphWriter {

    private static final Logger log = Logger.getLogger(CsvOsmGraphWriter.class.getName());
    private OSMGraph<OSMNode, OSMLink> graph;

    public CsvOsmGraphWriter() {
    }

    public void serializeTo(OSMGraph g, File nodes, File edges) throws IOException {
        this.graph = g;
        serializeNodes(nodes);
        serializeWays(edges);
    }

    private void serializeWays(File edges) throws IOException {
        log.fine("Serializing edges");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(edges, false), "UTF8"), 5 * 1024 * 1024); // 5MB filebuffer
        for (OSMLink<OSMNode> link : graph.getLinkList()) {
            bw.append("id=" + link.getId());
            for (OSMNode node : link.getNodes()) {
                bw.append(",node=" + node.getName());
            }
            Map<String, String> attr = link.getAttr();
            for (Map.Entry<String, String> entry : attr.entrySet()) {
                assert !entry.getKey().equals("node") : "node in properties for link " + link;
                bw.append("," + entry.getKey() + "=\"" + entry.getValue() + "\"");
            }
            bw.newLine();
        }
        bw.close();
    }

    private void serializeNodes(File nodes) throws IOException {
        log.fine("Serializing nodes with links");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(nodes, false), "UTF8"), 5 * 1024 * 1024); // 5MB filebuffer
        for (OSMNode node : graph.getNodes()) {
            bw.append(nodeToString(node));
            bw.newLine();
        }
        log.fine("Serializing nodes without links");
        for (OSMNode node : graph.getNodesWithoutLinks()) {
            bw.append(nodeToString(node));
            bw.newLine();
        }
        bw.close();
    }

    private String nodeToString(OSMNode node) {
        StringBuilder bw = new StringBuilder(50);
        bw.append(String.format(Locale.US, "id=%d,lat=%f,lon=%f",
                node.getName(), node.getLat(), node.getLon()));
        if (node.getHeight() > 0) {
            bw.append(String.format(Locale.US, ",height=%.3f", node.getHeight()));
        }
        Map<String, String> attr = node.getAttr();
        for (Map.Entry<String, String> entry : attr.entrySet()) {
            bw.append("," + entry.getKey() + "=\"" + entry.getValue() + "\"");
        }
        return bw.toString();
    }
}
