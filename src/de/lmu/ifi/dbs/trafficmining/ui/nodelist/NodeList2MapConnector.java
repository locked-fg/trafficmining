package de.lmu.ifi.dbs.trafficmining.ui.nodelist;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.ui.MapWrapper;
import de.lmu.ifi.dbs.trafficmining.utils.OSMUtils;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 *
 * @author Franz
 */
public class NodeList2MapConnector extends MouseAdapter {

    private final NodeListPanel nodes;
    private final MapWrapper map;
    private Graph graph;

    public NodeList2MapConnector(NodeListPanel nodes, MapWrapper map) {
        assert nodes != null : "nodes must not be null";
        assert map != null : "map must not be null";

        this.nodes = nodes;
        this.map = map;

        this.map.addMouseListener(this);
        this.nodes.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                updateMap();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                updateMap();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                updateMap();
            }
        });
    }

    private void updateMap() {
        map.setNodes(NodeList2MapConnector.class, nodes.getNodes());
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (graph == null) {
            return;
        }
        if (e.getClickCount() >= 2) {
            Point p = e.getPoint();
            GeoPosition geo = map.xyToGeo(p);
            Node node = OSMUtils.getNearestNode(geo, graph);
            if (node != null) {
                nodes.addNode(node);
            }
        }
    }
}
