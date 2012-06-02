package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.graph.Node;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import org.jdesktop.swingx.mapviewer.Waypoint;

/**
 * List Model that can ONLY store OSMNodes or objects that extend OSMNode. The
 * model is used for the jList representing the node list which is handed over
 * to the algorithm.
 *
 * An IllegalArgumentException will be thrown if objects other than OSMNode are
 * added.
 *
 * @author graf
 * @see OSMNode
 */
public class NodeListModel extends DefaultListModel {

    /**
     * add a node at the corrsponding index
     *
     * @param index the position to store the node
     * @param element the node to be added
     * @see DefaultListModel#add(int, java.lang.Object)
     * @throws IllegalArgumentException if element is not an OSMNode
     */
    @Override
    public void add(int index, Object element) {
        if (!Node.class.isAssignableFrom(element.getClass())) {
            throw new IllegalArgumentException("element MUST be of type OSMNode");
        }
        super.add(index, element);
    }

    /**
     * add a node at the end of the list
     *
     * @param obj the node to be added
     * @see DefaultListModel#addElement(java.lang.Object)
     * @throws IllegalArgumentException if element is not an OSMNode
     */
    @Override
    public void addElement(Object obj) {
        if (!Node.class.isAssignableFrom(obj.getClass())) {
            throw new IllegalArgumentException("element MUST be of type OSMNode");
        }
        super.addElement(obj);
    }

    /**
     * @return list of OSMNodes added to the model
     */
    public List<Node> getNodes() {
        List<Node> result = new ArrayList<>(getSize());
        for (int i = 0; i < getSize(); i++) {
            Object o = get(i);
            if (o instanceof Node) {
                result.add((Node) o);
            }
        }
        return result;

    }

    /**
     * @return List of Waypoints which are created from the OSMNodes
     */
    public List<Waypoint> getWaypoints() {
        List<Waypoint> result = new ArrayList<>(getSize());
        for (Node node : getNodes()) {
            result.add(new Waypoint(node.getGeoPosition()));
        }
        return result;
    }
}