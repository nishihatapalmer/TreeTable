package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TreeTableCellRenderer extends DefaultTableCellRenderer {

    //TODO: should indent be defined in terms of pixels (HDPi screens will make this very small...)
    private static final int PIXELS_PER_LEVEL = 16;

    private final TreeTableModel treeTableModel;

    private Icon expandedIcon;
    private Icon collapsedIcon;
    private int maxIconWidth;

    //TODO: validate constants set here.
    private final Insets insets = new Insets(0, 24, 0, 0);

    private TreeTableNode currentNode;

    public TreeTableCellRenderer(final TreeTableModel treeTableModel) {
        this.treeTableModel = treeTableModel;
        setExpandedIcon(UIManager.getIcon("Tree.expandedIcon"));
        setCollapsedIcon(UIManager.getIcon("Tree.collapsedIcon"));
        setBorder(new ExpandHandleBorder());
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
        setForeground(isSelected? table.getSelectionForeground() : table.getForeground());
        setBackground(isSelected? table.getSelectionBackground() : table.getBackground());
        setFont(table.getFont());
        setValue(value);
        setToolTipText(value.toString());
        setNode(getNodeAtRow(table, row));
        return this;
    }

    public void setExpandedIcon(final Icon expandedIcon) {
        this.expandedIcon = expandedIcon;
        setMaxIconWidth();
    }

    public void setCollapsedIcon(final Icon expandedIcon) {
        this.collapsedIcon = expandedIcon;
        setMaxIconWidth();
    }

    protected TreeTableNode getNodeAtRow(final JTable table, final int row) {
        return treeTableModel.getNodeAtTableRow(table, row);
    }

    protected int getNodeIndent(final TreeTableNode node) {
        final int adjustShowRoot = treeTableModel.getShowRoot()? 0 : 1;
        return maxIconWidth + ((node.getLevel() - adjustShowRoot) * PIXELS_PER_LEVEL);
    }

    private void setNode(final TreeTableNode node) {
        insets.left = getNodeIndent(node);
        currentNode = node;
    }

    private void setMaxIconWidth() {
        int expandedWidth = expandedIcon == null? 0 : expandedIcon.getIconWidth();
        int collapsedWidth = collapsedIcon == null? 0 : collapsedIcon.getIconWidth();
        maxIconWidth = Math.max(expandedWidth, collapsedWidth);
    }

    /**
     * A class which provides an expanded border to render the tree expand/collapse handle icon, indented by insets.left.
     */
    private class ExpandHandleBorder implements Border {

        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y,
                                final int width, final int height) {
            if (currentNode.getAllowsChildren()) {
                Icon iconToPaint = currentNode.isExpanded() ? expandedIcon : collapsedIcon;
                iconToPaint.paintIcon(c, g, insets.left - iconToPaint.getIconWidth(), 0);
            }
        }

        @Override
        public Insets getBorderInsets(final Component c) {
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

}
