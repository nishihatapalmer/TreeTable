/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2021, Matt Palmer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Renders a tree column, including collapse/expand handles, and an icon if supplied by the model
 */
public class TreeTableCellRenderer extends DefaultTableCellRenderer implements TreeTableModel.TreeClickHandler {

    private static final int PADDING = 4;  // how many pixels to pad left and right, so display looks nice.
    private static final int DEFAULT_PIXELS_PER_LEVEL = 16; // how many pixels to indent for each level of tree node.

    protected final TreeTableModel treeTableModel;
    protected final Insets insets = new Insets(0, 0, 0, 0);

    protected int pixelsPerLevel = DEFAULT_PIXELS_PER_LEVEL;
    protected Icon expandedIcon;  // expand icon, dependent on the look and feel theme.
    protected Icon collapsedIcon; // collapse icon, dependent on the look and feel theme.

    // We have labels for each of the icons, because for some reason GTK icons won't paint on Graphic objects,
    // but when embedded in a JLabel it paints fine.  They seem to be some kind of Icon proxy object...
    protected JLabel expandedIconLabel;
    protected JLabel collapsedIconLabel;

    protected int maxIconWidth; // Calculated max icon width of the expand and collapse icons, to get consistent indentation levels.
    protected TreeNode currentNode; // The node about to be rendered.

    /**
     * Constructs a TreeTableCellRenderer given a TreeTableModel.
     *
     * @param treeTableModel The model to render cells for.
     */
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
        final TreeNode node = treeTableModel.getNodeAtTableRow(table, row);
        setNode(node);
        setNodeIcon(node);
        return this;
    }

    @Override
    public boolean clickOnExpand(TreeNode node, int column, MouseEvent evt) {
        final TreeTableModel localModel = treeTableModel;
        final TableColumnModel columnModel = localModel.getTableColumnModel();
        final int columnModelIndex = columnModel.getColumn(column).getModelIndex();
        if (columnModelIndex == localModel.getTreeColumn() && node != null & node.getAllowsChildren()) {
            final int columnStart = calculateWidthToLeft(columnModel, column);
            final int expandEnd = columnStart + getNodeIndent(node);
            final int mouseX = evt.getPoint().x;
            if (mouseX > columnStart && mouseX < expandEnd) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The number of pixels per level of tree indent.
     */
    public int getPixelsPerLevel() {
        return pixelsPerLevel;
    }

    /**
     * Sets the number of pixels per level of tree indent.
     * @param pixelsPerLevel the number of pixels per level of tree indent.
     */
    public void setPixelsPerLevel(final int pixelsPerLevel) {
        this.pixelsPerLevel = pixelsPerLevel;
    }

    /**
     * Sets the icon to use to indicate an expanded tree node.
     * @param expandedIcon the icon to use to indicate an expanded tree node.
     */
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

    /**
     * Sets the icon to use to indicate a collapsed tree node.
     * @param collapsedIcon the icon to use to indicate a collapsed tree node.
     */
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + '('+ treeTableModel + ')';
    }

    protected int getNodeIndent(final TreeNode node) {
        final int adjustShowRoot = treeTableModel.getShowRoot()? 0 : 1;
        return PADDING + maxIconWidth + ((getLevel(node) - adjustShowRoot) * pixelsPerLevel);
    }

    protected final void setNode(final TreeNode node) {
        insets.left = getNodeIndent(node);
        currentNode = node;
    }

    protected final void setNodeIcon(final TreeNode node) {
        setIcon(treeTableModel.getNodeIcon(node));
    }

    protected final void setMaxIconWidth() {
        int expandedWidth = expandedIcon == null? 0 : expandedIcon.getIconWidth();
        int collapsedWidth = collapsedIcon == null? 0 : collapsedIcon.getIconWidth();
        maxIconWidth = Math.max(expandedWidth, collapsedWidth);
    }

    protected final int getLevel(final TreeNode node) {
        int nodeLevel = 0;
        TreeNode currentNode = node;
        while ((currentNode = currentNode.getParent()) != null) {
            nodeLevel++;
        }
        return nodeLevel;
    }

    /**
     * Calculates the space taken up by columns to the left of the column in the TableColumnModel.
     *
     * @param colIndex
     * @return
     */
    protected final int calculateWidthToLeft(final TableColumnModel columnModel, final int colIndex) {
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += columnModel.getColumn(col).getWidth();
        }
        return width;
    }

    /**
     * A class which provides an expanded border to render the tree expand/collapse handle icon, indented by insets.left.
     */
    protected class ExpandHandleBorder implements Border {

        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y,
                                final int width, final int height) {
            if (currentNode.getAllowsChildren()) {
                final JLabel labelToPaint = treeTableModel.isExpanded(currentNode) ? expandedIconLabel : collapsedIconLabel;
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
