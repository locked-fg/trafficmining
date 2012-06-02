package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import javax.swing.JList;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.WaypointPainter;

/**
 * MouseAdapter that takes a click on the main map, seeks the nearest node to
 * the click position in the graph and adds this node in the according lists.
 */
public class MapToNodeList extends MouseAdapter {

    private final Graph<Node<Link>, Link<Node>> graph;
    private final NodeListModel nodeListModel;
    private final JList nodeWaypointList;
    private final WaypointPainter<JXMapViewer> startEndPainter;

    public MapToNodeList(Graph<Node<Link>, Link<Node>> graph,
            JList waypointList, WaypointPainter<JXMapViewer> startEndPainter) {
        this.graph = graph;
        this.nodeWaypointList = waypointList;
        this.nodeListModel = (NodeListModel) this.nodeWaypointList.getModel();
        this.startEndPainter = startEndPainter;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JXMapViewer map = (JXMapViewer) e.getSource();
        GeoPosition pos = map.convertPointToGeoPosition(e.getPoint());
        Node node = OSMUtils.getNearestNode(pos, graph);
        if (nodeListModel.contains(node)) {
            nodeListModel.removeElement(node);
        } else {
            List<Link> links = node.getLinks();
            String name = new String();
            for (Link oSMLink : links) {
                String l_name = oSMLink.getAttr("name");
                if (l_name != null && !name.contains(l_name)) {
                    name += l_name + "; ";
                }
            }
            node.setName(name);
            nodeListModel.addElement(node);
        }
        nodeWaypointList.ensureIndexIsVisible(nodeListModel.size() - 1);
        startEndPainter.setWaypoints(new HashSet<>(nodeListModel.getWaypoints()));
        map.repaint();
    }
}