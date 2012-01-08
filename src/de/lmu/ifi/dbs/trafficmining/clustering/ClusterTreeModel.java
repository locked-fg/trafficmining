
package de.lmu.ifi.dbs.trafficmining.clustering;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author skurtz
 */
public class ClusterTreeModel implements TreeModel{
    private Cluster root = null;

    public ClusterTreeModel(Cluster root) {
        this.root = root;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object o, int i) {
        if(!(root==null) && i<root.size()){
            return ((Cluster)o).getChild(i);
        }
        else{
            return null;
        }
    }

    @Override
    public int getChildCount(Object o) {
        if(o instanceof Cluster) return ((Cluster)o).size();
        return 0;
    }

    @Override
    public boolean isLeaf(Object o) {
        return (o instanceof Route);
    }

    @Override
    public void valueForPathChanged(TreePath tp, Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addTreeModelListener(TreeModelListener tl) {}

    @Override
    public void removeTreeModelListener(TreeModelListener tl) {}

}
