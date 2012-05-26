package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.OSMNodeListModel;
import de.lmu.ifi.dbs.trafficmining.graph.OSMGraph;
import de.lmu.ifi.dbs.trafficmining.graph.OSMLink;
import de.lmu.ifi.dbs.trafficmining.graph.OSMNode;
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

    private final OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph;
    private final OSMNodeListModel nodeListModel;
    private final JList nodeWaypointList;
    private final WaypointPainter<JXMapViewer> startEndPainter;

    public MapToNodeList(OSMGraph<OSMNode<OSMLink>, OSMLink<OSMNode>> graph,
            JList waypointList, WaypointPainter<JXMapViewer> startEndPainter) {
        this.graph = graph;
        this.nodeWaypointList = waypointList;
        this.nodeListModel = (OSMNodeListModel) this.nodeWaypointList.getModel();
        this.startEndPainter = startEndPainter;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JXMapViewer map = (JXMapViewer) e.getSource();
        GeoPosition pos = map.convertPointToGeoPosition(e.getPoint());
        OSMNode node = OSMUtils.getNearestNode(pos, graph);
        if (nodeListModel.contains(node)) {
            nodeListModel.removeElement(node);
        } else {
            List<OSMLink> links = node.getLinks();
            String name = new String();
            for (OSMLink oSMLink : links) {
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