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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;

/**
 * Renders a tree column, including collapse/expand handles, and an icon if supplied by the model.
 * It also implements the TreeTableModel.TreeClickHandler, so it can respond to clicks where the expand or collapse
 * handles are drawn for each node.
 */
public class TreeCellRenderer extends DefaultTableCellRenderer implements TreeTableModel.TreeClickHandler {

    protected static final int TREE_COLUMN_MODEL_INDEX = 0;

    /**
     * How many pixels to pad left and right, so display looks nice.
     */
    protected static final int PADDING = 4;

    /**
     * How many pixels to indent for each level of tree node.
     */
    protected static final int DEFAULT_PIXELS_PER_LEVEL = 16;

    /**
     * The TreeTableModel this renderer uses to obtain tree nodes.
     */
    protected final TreeTableModel treeTableModel;

    /**
     * The current indentations at the border of the tree cell being rendered.
     * The left inset is set to indent the tree nodes.
     */
    protected final Insets insets = new Insets(0, 0, 0, 0);

    /**
     * The number of pixels to indent per level of tree node.
     */
    protected int pixelsPerLevel = DEFAULT_PIXELS_PER_LEVEL;

    /**
     * The label used to render expand or collapse handle icons.
     *
     * We use a JLabel to render the expand and collapse icons,
     * because for some reason GTK icons won't paint on Graphic objects.
     * When embedded in a JLabel they paint fine.  They seem to be using some kind of Icon proxy object...
     */
    protected final JLabel expandCollapseIconRenderer;

    /**
     * The expand icon, dependent on the look and feel theme.
     * You can set your own icon using {@link #setExpandedIcon(Icon)}
     */
    protected Icon expandedIcon;

    /**
     * The collapse icon, dependent on the look and feel theme.
     * You can set your own icon using {@link #setCollapsedIcon(Icon)}
     */
    protected Icon collapsedIcon;

    /**
     * Calculated max icon width of expand and collapse icons, to get consistent indentation levels.
     * when expanded or collapsed.
     */
    protected int maxIconWidth;

    /**
     * The current node about to be rendered.  Must be set before any methods that depend on it.
     * It is set as the first action in {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
     */
    protected TreeNode currentNode;

    /**
     * Constructs a TreeCellRenderer given a TreeTableModel.
     *
     * @param treeTableModel The model to render cells for.
     * @throws IllegalArgumentException if the treeTableModel is null.
     */
    public TreeCellRenderer(final TreeTableModel treeTableModel) {
        if (treeTableModel == null) {
            throw new IllegalArgumentException("TreeTableModel cannot be null.");
        }
        this.treeTableModel = treeTableModel;
        expandCollapseIconRenderer = new JLabel("", SwingConstants.RIGHT);
        expandCollapseIconRenderer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, PADDING));
        setExpandedIcon(UIManager.getIcon("Tree.expandedIcon"));
        setCollapsedIcon(UIManager.getIcon("Tree.collapsedIcon"));
        setBorder(new ExpandHandleBorder()); // The border paints the expand/collapse handles and handles the indentation of the node from the left.
    }

    /**
     * If overriding the TreeCellRenderer, preferably override the get methods called in this method, or
     * override {@link #setAdditionalProperties(TreeNode, JTable, Object, boolean, boolean, int, int)}
     * to set any additional properties, rather than overriding this method and calling super or re-implementing it.
     * <p>
     * All of the common actions such as changing values, colors, fonts, icons, tooltips values have dedicated methods
     * which can be separately overridden.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
        setNode(treeTableModel.getNodeAtTableRow(row)); // Ensure current node is set before anything else.
        setForeground( isSelected? getSelectedForegroundColor(table, value, hasFocus, row, column)
                                 : getUnselectedForegroundColor(table, value, hasFocus, row, column));
        setBackground( isSelected? getSelectedBackgroundColor(table, value, hasFocus, row, column)
                                 : getUnselectedBackgroundColor(table, value, hasFocus, row, column));
        setFont( getFont(table, value, isSelected, hasFocus, row, column));
        setValue( getValue(table, value, isSelected, hasFocus, row, column));
        setToolTipText( getToolTipText(table, value, isSelected, hasFocus, row, column));
        setNodeIndent( calculateNodeIndent(currentNode, table, value, isSelected, hasFocus, row, column));
        setIcon( getNodeIcon(currentNode, table, value, isSelected, hasFocus, row, column));
        setAdditionalProperties(currentNode, table, value, isSelected, hasFocus, row, column);
        return this;
    }

    @Override
    public boolean clickOnExpand(final TreeNode node, final int column, final MouseEvent evt) {
        final TableColumnModel columnModel = treeTableModel.getTableColumnModel();
        final int columnModelIndex = columnModel.getColumn(column).getModelIndex();
        if (columnModelIndex == TREE_COLUMN_MODEL_INDEX && node != null && node.getAllowsChildren()) {
            final int columnStart = TreeUtils.calculateWidthToLeftOfColumn(columnModel, column);
            final int expandEnd = columnStart + calculateNodeIndent(node);
            final int mouseX = evt.getPoint().x;
            return mouseX > columnStart && mouseX < expandEnd;
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
        this.expandedIcon = expandedIcon;
        setMaxIconWidth();
    }

    /**
     * Sets the icon to use to indicate a collapsed tree node.
     * @param collapsedIcon the icon to use to indicate a collapsed tree node.
     */
    public void setCollapsedIcon(final Icon collapsedIcon) {
        this.collapsedIcon = collapsedIcon;
        setMaxIconWidth();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '('+ treeTableModel + ')';
    }

    /**
     * @param currentNode The current node to be rendered.
     * @param table The JTable it is rendered in.
     * @param value The value of the cell (e.g. name or other column value).
     * @param isSelected Whether the cell is selected.
     * @param hasFocus Whether the cell has focus.
     * @param row The row of the cell being rendered.
     * @param column The column of the cell being rendered.
     * @return How many pixels to indent a tree node.
     */
    protected int calculateNodeIndent(final TreeNode currentNode, final JTable table, final Object value,
                                      final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return calculateNodeIndent(currentNode);
    }

    /**
     * @return The current node indent set.
     */
    protected int getNodeIndent() {
        return insets.left;
    }

    /**
     * Sets the indent for the current node.
     * @param indent The ident to set.
     */
    protected void setNodeIndent(final int indent) {
        insets.left = indent;
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The foreground color to use for a selected cell.
     */
    protected Color getSelectedForegroundColor(final JTable table, final Object value,
                                               final boolean hasFocus, final int row, final int column) {
        return table.getSelectionForeground();
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The foreground color to use for an unselected cell.
     */
    protected Color getUnselectedForegroundColor(final JTable table, final Object value,
                                                 final boolean hasFocus, final int row, final int column) {
        return table.getForeground();
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The background color to use for a selected cell.
     */
    protected Color getSelectedBackgroundColor(final JTable table, final Object value,
                                               final boolean hasFocus, final int row, final int column) {
        return table.getSelectionBackground();
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The background color to use for an unselected cell.
     */
    protected Color getUnselectedBackgroundColor(final JTable table, final Object value,
                                                 final boolean hasFocus, final int row, final int column) {
        return table.getBackground();
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param isSelected Whether it is selected.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The text to use for a tooltip for this cell.
     */
    protected String getToolTipText(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return value == null ? "" : value.toString();
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param isSelected Whether it is selected.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The value to render for this cell.
     */
    protected Object getValue(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return value;
    }

    /**
     * @param table The table
     * @param value The value of the current cell.
     * @param isSelected Whether it is selected.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     *
     * @return The font to use to render the cell.
     */
    protected Font getFont(final JTable table, final Object value, final boolean isSelected,
                           final boolean hasFocus, final int row, final int column) {
        return table.getFont();
    }

    /**
     * Override this method to set any additional properties on the label before it is rendered.
     * This is a blank implementation that does nothing.
     *
     * @param treeNode the tree node to be rendered.
     * @param table The table
     * @param value The value of the current cell.
     * @param isSelected Whether it is selected.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     */
    protected void setAdditionalProperties(final TreeNode treeNode, final JTable table, final Object value,
                                           final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        // No implementation here; subclasses can use this to set whatever addditional properties they like.
    }

    /**
     * @return The node which will be rendered if painted.
     */
    protected TreeNode getCurrentNode() {
        return currentNode;
    }

    /**
     * Sets the current node in the tree being rendered.
     * @param node The node to set.
     */
    protected final void setNode(final TreeNode node) {
        currentNode = node;
    }

    /**
     * Gets the icon for the node.  By default, this implementation fetches the node from the tree table model,
     * which subclasses of it can override to get the right tree icon.  If you prefer, you can instead override
     * this method in your TreeCellRenderer sub-class to get the right icon for the node.
     *
     * @param node The node to get the icon for.
     * @return The icon for the node, or null if no icon exists for it.
     */
    protected Icon getNodeIcon(final TreeNode node) {
        return treeTableModel.getNodeIcon(node);
    }

    /**
     * @param node the tree node to be rendered.
     * @param table The table
     * @param value The value of the current cell.
     * @param isSelected Whether it is selected.
     * @param hasFocus   Whether it has focus.
     * @param row        The row of the table
     * @param column     The column of the table.
     * @return The icon for the tree node.
     */
    protected final Icon getNodeIcon(final TreeNode node, final JTable table, final Object value,
                                     final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return getNodeIcon(node);
    }

    /**
     * Sets the maximum icon width to be the biggest of the expand icon and the collapse icon.
     */
    protected final void setMaxIconWidth() {
        int expandedWidth = expandedIcon == null? 0 : expandedIcon.getIconWidth();
        int collapsedWidth = collapsedIcon == null? 0 : collapsedIcon.getIconWidth();
        maxIconWidth = Math.max(expandedWidth, collapsedWidth);
    }

    /**
     * @return The number of pixels to indent for a node.
     */
    protected int calculateNodeIndent(final TreeNode node) {
        final int adjustShowRoot = treeTableModel.getShowRoot()? 0 : 1;
        return PADDING + maxIconWidth + ((TreeUtils.getLevel(node) - adjustShowRoot) * pixelsPerLevel);
    }

    /**
     * A class which provides an expanded border to render the tree expand/collapse handle icon, indented by insets.left.
     */
    protected class ExpandHandleBorder implements Border {

        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y,
                                final int width, final int height) {
            if (currentNode.getAllowsChildren()) {
                final JLabel localRenderer = expandCollapseIconRenderer;
                localRenderer.setIcon(treeTableModel.isExpanded(currentNode) ? expandedIcon : collapsedIcon);
                localRenderer.setSize(insets.left, height);
                localRenderer.paint(g);
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
