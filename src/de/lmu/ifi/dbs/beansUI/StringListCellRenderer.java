package de.lmu.ifi.dbs.beansUI;

import java.awt.Component;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author graf
 */
public class StringListCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof List && !(value instanceof NiceToStringList)) {
            value = new NiceToStringList((List) value);
            setValue(value);
        }
        return comp;
    }
}
