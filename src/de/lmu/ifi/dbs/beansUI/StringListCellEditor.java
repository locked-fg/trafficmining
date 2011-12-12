package de.lmu.ifi.dbs.beansUI;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyEditor;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.border.LineBorder;

public class StringListCellEditor extends DefaultCellEditor {

    private PropertyEditor pEditor;

    public StringListCellEditor() {
        super(new JComboBox());

        final JComboBox comboBox = (JComboBox) getComponent();
        delegate = new EditorDelegate() {

            @Override
            public void setValue(Object value) {
                if (value instanceof List) {
                    List vals = (List) value;
                    if (!vals.isEmpty()) {
                        comboBox.setSelectedItem(vals.get(0));
                    }
                }
                comboBox.setSelectedItem(value);
            }

            @Override
            public Object getCellEditorValue() {
                Object item = comboBox.getSelectedItem();
                pEditor.setAsText(item.toString());
                return pEditor.getValue();
            }
        };
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        JComboBox comboBox = (JComboBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
        comboBox.setBorder(new LineBorder(Color.black, 1));
        pEditor = ((BeanModel) table.getModel()).getPropertyEditor(row, column);
        comboBox.setModel(new DefaultComboBoxModel(pEditor.getTags()));
        comboBox.setSelectedItem(value);
        return comboBox;

    }
}
