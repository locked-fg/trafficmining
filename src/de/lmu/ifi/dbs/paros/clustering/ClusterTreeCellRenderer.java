/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.paros.clustering;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * @author wombat
 */
public class ClusterTreeCellRenderer extends DefaultTreeCellRenderer {

    public ClusterTreeCellRenderer() {
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);
        if (value instanceof Route) {
            List<String> units = ((Route) value).getUnits();
            List<Double> costs = ((Route) value).getMaxCosts();
            StringBuilder stb = new StringBuilder();
            int i = 0;
            DecimalFormat f = new DecimalFormat("0.00");

            stb.append("statistic data: ");
            for (i = 0; i < units.size() - 1; i++) {
                stb.append(f.format(costs.get(i)));
                stb.append(' ');
                stb.append(units.get(i));
                stb.append(", ");
            }
            stb.append(f.format(costs.get(i)));
            stb.append(' ');
            stb.append(units.get(i));
            setToolTipText(stb.toString());
        }
        if (value instanceof Cluster) {
            List<String> units = ((Cluster) value).getUnits();
            List<Double> minCosts = ((Cluster) value).getMinCosts();
            List<Double> maxCosts = ((Cluster) value).getMaxCosts();
            StringBuilder stb = new StringBuilder();
            int i = 0;
            DecimalFormat f = new DecimalFormat("0.00");

            stb.append("<html>statistic data: <br/>");
            stb.append("upper bound: ");
            for (i = 0; i < units.size() - 1; i++) {
                stb.append(f.format(maxCosts.get(i)));
                stb.append(' ');
                stb.append(units.get(i));
                stb.append(", ");
            }
            stb.append(f.format(maxCosts.get(i)));
            stb.append(' ');
            stb.append(units.get(i));
            stb.append("<br/>");

            stb.append("lower bound: ");
            for (i = 0; i < units.size() - 1; i++) {
                stb.append(f.format(minCosts.get(i)));
                stb.append(" ");
                stb.append(units.get(i));
                stb.append(", ");
            }
            stb.append(f.format(minCosts.get(i)));
            stb.append(" ");
            stb.append(units.get(i));
            stb.append("</html>");
            setToolTipText(stb.toString());
        }

        return this;
    }
}
