package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TreeTableCellRenderer extends DefaultTableCellRenderer {

    private static final int PIXELS_PER_LEVEL = 16;

    private final TreeTableModel treeTableModel;

    private Icon expandedIcon;
    private Icon collapsedIcon;
    private int maxIconWidth;

    private final Insets insets = new Insets(0, 24, 0, 0);

    private TreeTableNode currentNode;

    public TreeTableCellRenderer(TreeTableModel treeTableModel) {
        this.treeTableModel = treeTableModel;
        setExpandedIcon(UIManager.getIcon("Tree.expandedIcon"));
        setCollapsedIcon(UIManager.getIcon("Tree.collapsedIcon"));
        setBorder(new ExpandHandleBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setBackground(isSelected? table.getSelectionBackground() : table.getBackground());
        setValue(value);
        setNode(getNodeAtRow(row));
        return this;
    }

    public void setExpandedIcon(Icon expandedIcon) {
        this.expandedIcon = expandedIcon;
        setMaxIconWidth();
    }

    public void setCollapsedIcon(Icon expandedIcon) {
        this.collapsedIcon = expandedIcon;
        setMaxIconWidth();
    }

    protected TreeTableNode getNodeAtRow(final int row) {
        return treeTableModel.getNodeAtRow(row);
    }

    protected int getNodeIndent(TreeTableNode node) {
        final int adjustShowRoot = treeTableModel.getShowRoot()? 0 : 1;
        return maxIconWidth + ((node.getLevel() - adjustShowRoot) * PIXELS_PER_LEVEL);
    }

    private void setNode(TreeTableNode node) {
        insets.left = getNodeIndent(node);
        currentNode = node;
    }

    private void setMaxIconWidth() {
        int expandedWidth = expandedIcon == null? 0 : expandedIcon.getIconWidth();
        int collapsedWidth = collapsedIcon == null? 0 : collapsedIcon.getIconWidth();
        maxIconWidth = Math.max(expandedWidth, collapsedWidth);
    }

    private class ExpandHandleBorder implements Border {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (currentNode.getAllowsChildren()) {
                Icon iconToPaint = currentNode.isExpanded() ? expandedIcon : collapsedIcon;
                iconToPaint.paintIcon(c, g, insets.left - iconToPaint.getIconWidth(), 0);
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
