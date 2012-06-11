package de.lmu.ifi.dbs.beansUI;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class BeanTable extends JTable {

    public BeanTable() {
        setModel(new BeanModel());
        setDefaultEditor(Enum.class, new EnumCellEditor());
        //-
        setDefaultEditor(Color.class, new ColorCellEditor());
        setDefaultRenderer(Color.class, new ColorRenderer());
        //-
        setDefaultEditor(List.class, new StringListCellEditor());
        setDefaultRenderer(List.class, new StringListCellRenderer());
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        BeanModel model = (BeanModel) getModel();
        TableCellEditor editor = getDefaultEditor(model.getPropertyType(row, column));
        if (editor != null) { // checkboxes left aligned
            if (editor instanceof DefaultCellEditor) {
                Component c = ((DefaultCellEditor) editor).getComponent();
                if (c instanceof JCheckBox) {
                    ((JCheckBox) c).setHorizontalAlignment(JLabel.LEFT);
                }
            }
        }
        return editor;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        BeanModel model = (BeanModel) getModel();
        Class clazz = model.getPropertyType(row, column);

        TableCellRenderer r = getDefaultRenderer(clazz);
        if (r instanceof JCheckBox) { // just make the checkboxes left aligned
            ((JCheckBox) r).setHorizontalAlignment(JLabel.LEFT);
        }
        return r;
    }

    /**
     * delegate to ((BeanModel) getModel()).setBean(newBean);
     * @param newBean
     * @throws Exception
     */
    public void setBean(Object newBean) throws Exception {
        ((BeanModel) getModel()).setBean(newBean);
    }
}
