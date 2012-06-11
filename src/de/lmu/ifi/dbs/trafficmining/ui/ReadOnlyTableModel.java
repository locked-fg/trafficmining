package de.lmu.ifi.dbs.trafficmining.ui;

import javax.swing.table.DefaultTableModel;

/**
 * TableModel that does not allow editing of cells
 * @author Franz
 */
public class ReadOnlyTableModel extends DefaultTableModel {

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
