package de.lmu.ifi.dbs.trafficmining.result;

import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel;
import de.lmu.ifi.dbs.trafficmining.simplex.PointPanel.PointSource;
import de.lmu.ifi.dbs.trafficmining.simplex.SimplexControl;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * Link the clicks on the simplexcontrols' paintpanels with the selection of the
 * result list
 *
 * @author graf
 */
public class SimplexHighlighter extends MouseAdapter {

    private final JTable target;

    public SimplexHighlighter(JTable resultList) {
        this.target = resultList;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        SimplexControl control = (SimplexControl) e.getSource();
        PointPanel panel = control.getPointPanel();
        // convert to click on the panel
        e = SwingUtilities.convertMouseEvent((Component) control, e, control.getPointPanel());

        List<PointSource> pointSources2D = panel.getEpsilonPoints(e.getPoint(), panel.getPointSize() / 2d);
        panel.setHighlight(pointSources2D);
        final List<PointSource> pointSources3D = control.getSourceFor(pointSources2D);

        if (pointSources3D.size() > 0 && pointSources3D.get(0) instanceof SimplexResultEntry) {
            int[] resultIDs = new int[pointSources3D.size()];
            for (int i = 0; i < pointSources3D.size(); i++) {
                resultIDs[i] = ((SimplexResultEntry) pointSources3D.get(i)).getId();
            }

            int maxRow = target.getModel().getRowCount();
            target.getSelectionModel().clearSelection();
            for (int rowIndex = 0; rowIndex < maxRow; rowIndex++) {
                int id = (Integer) target.getValueAt(rowIndex, 0);
                if (Arrays2.indexOf(resultIDs, id) >= 0) {
                    target.getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
                }
            }
        }
    }
}
