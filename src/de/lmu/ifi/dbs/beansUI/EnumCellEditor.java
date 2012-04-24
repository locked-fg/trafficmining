package de.lmu.ifi.dbs.beansUI;

import java.awt.Component;
import java.beans.PropertyEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;

public class EnumCellEditor extends DefaultCellEditor {

    private PropertyEditor pEditor;

    public EnumCellEditor() {
        super(new JComboBox());

        final JComboBox comboBox = (JComboBox) getComponent();
        delegate = new EditorDelegate() {

            @Override
            public void setValue(Object value) {
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
        pEditor = ((BeanModel) table.getModel()).getPropertyEditor(row, column);
        comboBox.setModel(new DefaultComboBoxModel(pEditor.getTags()));
        comboBox.setSelectedItem(value);
        return comboBox;
    }
}
