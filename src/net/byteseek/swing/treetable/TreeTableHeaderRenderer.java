package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;

public class TreeTableHeaderRenderer extends JLabel implements TableCellRenderer {

    private Icon sortAscendingIcon;
    private Icon sortDescendingIcon;
    private int sortColumn;
    private SortOrder sortOrder;

    private final Insets insets = new Insets(2, 24, 2, 0);

    public TreeTableHeaderRenderer() {
        sortAscendingIcon = UIManager.getIcon("Table.ascendingSortIcon");
        sortDescendingIcon = UIManager.getIcon("Table.descendingSortIcon");
        setHorizontalAlignment(JLabel.CENTER);
        setBorder(new MatteBorder(1, 0, 1, 1, Color.lightGray));
        setMinimumSize(new Dimension(32, 64));
    }

    public Icon getSortAscendingIcon() {
        return sortAscendingIcon;
    }

    public Icon getSortDescendingIcon() {
        return sortDescendingIcon;
    }

    public void setSortAscendingIcon(Icon sortAscendingIcon) {
        this.sortAscendingIcon = sortAscendingIcon;
        //TODO: calc icon widths...?
    }

    public void setSortDescendingIcon(Icon sortAscendingIcon) {
        this.sortAscendingIcon = sortAscendingIcon;
        //TODO: calc icon widths...?
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(new CompoundBorder(border, new SortIconBorder()));
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setFont(header.getFont());
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                setText(value == null? "" : value.toString());
                setColumnSorted(table, column);
            }
        }
        return this;
    }

    private void setColumnSorted(JTable table, int column) {
        final RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
        if (rowSorter != null) {
            List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
            if (sortKeys != null) {
                for (int sortKeyIndex = 0; sortKeyIndex < sortKeys.size(); sortKeyIndex++) {
                    RowSorter.SortKey key = sortKeys.get(sortKeyIndex);
                    if (key.getColumn() == column && key.getSortOrder() != SortOrder.UNSORTED) {
                        sortOrder = key.getSortOrder();
                        sortColumn = sortKeyIndex;
                        setBold(true);
                        return;
                    }
                }
            }
        }
        setBold(false);
        sortOrder = SortOrder.UNSORTED;
        sortColumn = -1;
    }

    private void setBold(final boolean bold) {
        setFont(bold ? getBoldFont() : getUnBoldFont());
    }

    private Font getBoldFont() {
        Font currentFont = getFont();
        return currentFont.deriveFont(currentFont.getStyle() | Font.BOLD);
    }

    private Font getUnBoldFont() {
        Font currentFont = getFont();
        return currentFont.deriveFont(currentFont.getStyle() & ~Font.BOLD);
    }

    private class SortIconBorder implements Border {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (sortColumn >= 0) {
                Icon iconToPaint = sortOrder == sortOrder.ASCENDING ? sortAscendingIcon : sortDescendingIcon;
                iconToPaint.paintIcon(c, g, insets.left - iconToPaint.getIconWidth(), 4);
                if (g instanceof Graphics2D) {
                    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                                      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                }
                g.setFont(getUnBoldFont());
                g.setColor(Color.GRAY);
                g.drawString(Integer.toString(sortColumn + 1), 2, 14 + insets.top);
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
