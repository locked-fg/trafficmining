package de.lmu.ifi.dbs.beansUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyEditor;
import java.util.EventObject;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ColorCellEditor extends DefaultCellEditor {

    private PropertyEditor propertyEditor;
    private JColorChooser customEditor;
    private ColorRenderer renderer = new ColorRenderer();

    public ColorCellEditor() {
        super(new JTextField());

        delegate = new EditorDelegate() {

            @Override
            public void setValue(Object value) {
                // sets the initial color to the dialog's panel
                customEditor.setColor((Color) value);
            }
        };
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
        final BeanModel model = (BeanModel) table.getModel();
        propertyEditor = ((BeanModel) table.getModel()).getPropertyEditor(row, column);
        customEditor = (JColorChooser) propertyEditor.getCustomEditor();

        // fire up the dialog showing the color chooser
        final JDialog jDialog = new JDialog(SwingUtilities.windowForComponent(table));
        jDialog.setLayout(new BorderLayout());
        jDialog.add(customEditor, BorderLayout.CENTER);
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                jDialog.dispose();
            }
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(okButton);
        jDialog.add(south, BorderLayout.SOUTH);


        jDialog.pack();
        jDialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                Color paint = customEditor.getColor();
                model.setValueAt(paint, row, column);
                // if the dialog closes, we're still in edit mode and still show the renderer
                // which holds the wrong color - that's kinda strange here.
                // Removing this line causes the table to show the OLD color until the
                // table is resized.
                renderer.getTableCellRendererComponent(table, paint, false, false, row, column);
            }
        });
        jDialog.setVisible(true);

        super.getTableCellEditorComponent(table, value, isSelected, row, column);
        return renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column);
    }
}
//public class ColorCellEditor extends DefaultCellEditor {
//
//    private PropertyEditor propertyEditor;
//    private PaintPicker customEditor;
//    private ColorRenderer renderer = new ColorRenderer();
//
//    public ColorCellEditor() {
//        super(new JTextField());
//
//        delegate = new EditorDelegate() {
//
//            @Override
//            public void setValue(Object value) {
//                // sets the initial color to the dialog's panel
//                customEditor.setPaint((Paint) value);
//            }
//        };
//    }
//
//    @Override
//    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
//        final BeanModel model = (BeanModel) table.getModel();
//        propertyEditor = ((BeanModel) table.getModel()).getPropertyEditor(row, column);
//        customEditor = (PaintPicker) propertyEditor.getCustomEditor();
//
//        // fire up the dialog showing the color chooser
//        final JDialog jDialog = new JDialog(SwingUtilities.windowForComponent(table));
//        jDialog.setLayout(new BorderLayout());
//        jDialog.add(customEditor, BorderLayout.CENTER);
//        JButton okButton = new JButton("Ok");
//        okButton.addActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                jDialog.dispose();
//            }
//        });
//        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        south.add(okButton);
//        jDialog.add(south, BorderLayout.SOUTH);
//
//
//        jDialog.pack();
//        jDialog.addWindowListener(new WindowAdapter() {
//
//            @Override
//            public void windowClosed(WindowEvent e) {
//                Paint paint = customEditor.getPaint();
//                model.setValueAt(paint, row, column);
//                // if the dialog closes, we're still in edit mode and still show the renderer
//                // which holds the wrong color - that's kinda strange here.
//                // Removing this line causes the table to show the OLD color until the
//                // table is resized.
//                renderer.getTableCellRendererComponent(table, paint, false, false, row, column);
//            }
//        });
//        jDialog.setVisible(true);
//
//        super.getTableCellEditorComponent(table, value, isSelected, row, column);
//        return renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column);
//    }
//}

