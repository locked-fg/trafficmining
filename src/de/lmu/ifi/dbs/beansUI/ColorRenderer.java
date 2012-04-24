package de.lmu.ifi.dbs.beansUI;

import java.awt.*;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import sun.swing.DefaultLookup;

/**
 * @author graf
 */
class ColorRenderer extends JPanel implements TableCellRenderer {

    private final JPanel colorPanel;
    private final JLabel label;
    private final JButton button;
    private Color unselectedForeground;
    private Color unselectedBackground;

    public ColorRenderer() {
        setLayout(new BorderLayout(5, 0));
        colorPanel = new JPanel();
        colorPanel.setOpaque(true);
        // a bit ugly as it dows not adapt to the table
        Border b = new CompoundBorder(new LineBorder(Color.white, 1), new LineBorder(Color.black, 1));
        colorPanel.setBorder(b);
        colorPanel.setSize(5, 5);
        add(colorPanel, BorderLayout.WEST);

        label = new JLabel();
        label.setOpaque(false);
        add(label, BorderLayout.CENTER);

        // FIXME: this button should start editing on click!
        button = new JButton("...");
        button.setOpaque(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        add(button, BorderLayout.EAST);
    }

    public void setColor(Color c) {
        if (c == null) {
            return;
        }
        colorPanel.setBackground(c);
        label.setText(String.format("[%d,%d,%d]", c.getRed(), c.getGreen(), c.getBlue()));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Copied from DefaultTableCellRenderer
        Color fg = DefaultLookup.getColor(this, ui, "Table.dropCellForeground");
        Color bg = DefaultLookup.getColor(this, ui, "Table.dropCellBackground");
        if (isSelected) {
            setForeground(fg == null ? table.getSelectionForeground() : fg);
            setBackground(bg == null ? table.getSelectionBackground() : bg);
        } else {
            Color background = unselectedBackground != null ? unselectedBackground : table.getBackground();
            if (background == null || background instanceof javax.swing.plaf.UIResource) {
                Color alternateColor = DefaultLookup.getColor(this, ui, "Table.alternateRowColor");
                if (alternateColor != null && row % 2 == 0) {
                    background = alternateColor;
                }
            }
            setForeground(unselectedForeground != null
                    ? unselectedForeground
                    : table.getForeground());
            setBackground(background);
        }

        setColor((Color) value);
        return this;
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        colorPanel.setPreferredSize(new Dimension(h, h));
        super.setBounds(x, y, w, h);
    }
}
