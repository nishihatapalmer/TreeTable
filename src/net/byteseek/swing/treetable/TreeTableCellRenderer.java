package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TreeTableCellRenderer extends DefaultTableCellRenderer {

    private static final int PADDING = 4;
    private static final int PIXELS_PER_LEVEL = 18;

    private final TreeTableModel treeTableModel;
    private final Insets insets = new Insets(0, 0, 0, 0);

    private Icon expandedIcon;         // expand icon, dependent on the look and feel theme.
    private Icon collapsedIcon;        // collapse icon, dependent on the look and feel theme.
    private JLabel expandedIconLabel;  // For some reason, GTK icons don't paint directly, but they do inside a JLabel...
    private JLabel collapsedIconLabel; // For some reason, GTK icons don't paint directly, but they do inside a JLabel...
    private int pixelsPerLevel = PIXELS_PER_LEVEL;

    private int maxIconWidth;          // Calculated max icon width of the expand and collapse icons.

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

    public int getPixelsPerLevel() {
        return pixelsPerLevel;
    }

    public void setPixelsPerLevel(int pixelsPerLevel) {
        this.pixelsPerLevel = pixelsPerLevel;
    }

    public void setExpandedIcon(final Icon expandedIcon) {
        if (expandedIconLabel == null) {
            expandedIconLabel = new JLabel(expandedIcon, JLabel.RIGHT);
            expandedIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, PADDING));
        } else {
            expandedIconLabel.setIcon(expandedIcon);
        }
        this.expandedIcon = expandedIcon;
        setMaxIconWidth();
    }

    public void setCollapsedIcon(final Icon collapsedIcon) {
        if (collapsedIconLabel == null) {
            collapsedIconLabel = new JLabel(collapsedIcon, JLabel.RIGHT);
            collapsedIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, PADDING));
        } else {
            collapsedIconLabel.setIcon(collapsedIcon);
        }
        this.collapsedIcon = expandedIcon;
        setMaxIconWidth();
    }

    protected TreeTableNode getNodeAtRow(final JTable table, final int row) {
        return treeTableModel.getNodeAtTableRow(table, row);
    }

    protected int getNodeIndent(final TreeTableNode node) {
        final int adjustShowRoot = treeTableModel.getShowRoot()? 0 : 1;
        return PADDING + maxIconWidth + ((node.getLevel() - adjustShowRoot) * pixelsPerLevel);
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
                final JLabel labelToPaint = currentNode.isExpanded() ? expandedIconLabel : collapsedIconLabel;
                labelToPaint.setSize(insets.left, height);
                labelToPaint.paint(g);
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
