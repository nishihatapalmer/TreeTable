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
import java.awt.Font;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TreeCellRendererTest extends BaseTestClass  {

    protected TreeCellRenderer renderer;

    @Test
    public void testIllegalArgumentExceptionConstructWithNullModel() {
        assertThrows(IllegalArgumentException.class,
                ()-> new TreeCellRenderer(null));
    }

    @Test
    public void testSetGetCurrentNodeToRender() {
        renderer = new TreeCellRenderer(model);
        assertNull(renderer.getCurrentNode());

        renderer.setNode(rootNode);
        assertEquals(rootNode, renderer.getCurrentNode());

        renderer.setNode(null);
        assertNull(renderer.getCurrentNode());
    }

    @Test
    public void testSetCurrentNodeCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            renderer = new TreeCellRenderer(model);
            TreeNode expectedNode = model.getNodeAtTableRow(row);

            renderer.getTableCellRendererComponent(table, "test", false, false, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());

            renderer.setNode(null);
            renderer.getTableCellRendererComponent(table, "test", true, false, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());

            renderer.setNode(null);
            renderer.getTableCellRendererComponent(table, "test", true, true, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());

            renderer.setNode(null);
            renderer.getTableCellRendererComponent(table, "test", false, true, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            TreeNode expectedNode = model.getNodeAtTableRow(row);

            renderer.getTableCellRendererComponent(table, "test", false, false, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());

            renderer.setNode(null);
            renderer.getTableCellRendererComponent(table, "test", true, false, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());

            renderer.setNode(null);
            renderer.getTableCellRendererComponent(table, "test", true, true, row, 0);
            assertEquals(expectedNode, renderer.getCurrentNode());

            renderer.setNode(null);
            renderer.getTableCellRendererComponent(table, "test", false, true, row, 0);
        }
    }

    @Test
    public void testSetGetValueCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            String expectedValue = "test" + row;
            renderer = new TreeCellRenderer(model);

            renderer.getTableCellRendererComponent(table, expectedValue, false, false, row, 0);
            assertEquals(expectedValue, renderer.getText());
            assertEquals(expectedValue, renderer.getValue(table, expectedValue, false, false, row, 0));

            renderer.setText("");
            renderer.getTableCellRendererComponent(table, expectedValue, true, false, row, 0);
            assertEquals(expectedValue, renderer.getText());
            assertEquals(expectedValue, renderer.getValue(table, expectedValue, true, false, row, 0));

            renderer.setText("");
            renderer.getTableCellRendererComponent(table, expectedValue, true, true, row, 0);
            assertEquals(expectedValue, renderer.getText());

            renderer.setText("");
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getText());
            assertEquals(expectedValue, renderer.getValue(table, expectedValue, false, true, row, 0));
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String expectedValue = "test" + Integer.toString(row);

            renderer.getTableCellRendererComponent(table, expectedValue, false, false, row, 0);
            assertEquals(expectedValue, renderer.getText());
            assertEquals(expectedValue, renderer.getValue(table, expectedValue, false, false, row, 0));

            renderer.setText("");
            renderer.getTableCellRendererComponent(table, expectedValue, true, false, row, 0);
            assertEquals(expectedValue, renderer.getText());
            assertEquals(expectedValue, renderer.getValue(table, expectedValue, true, false, row, 0));

            renderer.setText("");
            renderer.getTableCellRendererComponent(table, expectedValue, true, true, row, 0);
            assertEquals(expectedValue, renderer.getText());

            renderer.setText("");
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getText());
            assertEquals(expectedValue, renderer.getValue(table, expectedValue, false, true, row, 0));
        }
    }

    @Test
    public void testSetGetFontCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            renderer = new TreeCellRenderer(model);

            Font expectedValue = renderer.getFont(table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getFont());
            assertEquals(expectedValue, renderer.getFont(table, description, false, false, row, 0));

            renderer.setFont(null);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getFont());
            assertEquals(expectedValue, renderer.getFont(table, description, true, false, row, 0));

            renderer.setFont(null);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getFont());

            renderer.setFont(null);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getFont());
            assertEquals(expectedValue, renderer.getFont(table, expectedValue, false, true, row, 0));
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            Font expectedValue = renderer.getFont(table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getFont());
            assertEquals(expectedValue, renderer.getFont(table, description, false, false, row, 0));

            renderer.setFont(null);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getFont());
            assertEquals(expectedValue, renderer.getFont(table, description, true, false, row, 0));

            renderer.setFont(null);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getFont());

            renderer.setFont(null);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getFont());
            assertEquals(expectedValue, renderer.getFont(table, expectedValue, false, true, row, 0));
        }
    }

    @Test
    public void testSetGetTooltipTextCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            renderer = new TreeCellRenderer(model);

            String expectedValue = renderer.getToolTipText(table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());
            assertEquals(expectedValue, renderer.getToolTipText(table, description, false, false, row, 0));

            renderer.setToolTipText(null);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());
            assertEquals(expectedValue, renderer.getToolTipText(table, description, true, false, row, 0));

            renderer.setToolTipText(null);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());

            renderer.setToolTipText(null);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());
            assertEquals(expectedValue, renderer.getToolTipText(table, expectedValue, false, true, row, 0));
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            String expectedValue = renderer.getToolTipText(table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());
            assertEquals(expectedValue, renderer.getToolTipText(table, description, false, false, row, 0));

            renderer.setToolTipText(null);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());
            assertEquals(expectedValue, renderer.getToolTipText(table, description, true, false, row, 0));

            renderer.setToolTipText(null);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());

            renderer.setToolTipText(null);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getToolTipText());
            assertEquals(expectedValue, renderer.getToolTipText(table, expectedValue, false, true, row, 0));
        }
    }

    @Test
    public void testSetGetNodeIndentCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            renderer = new TreeCellRenderer(model);
            TreeNode tableNode = model.getNodeAtTableRow(row);

            int expectedValue = renderer.calculateNodeIndent(tableNode, table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());

            renderer.setNodeIndent(-1);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());

            renderer.setNodeIndent(-1);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());

            renderer.setNodeIndent(-1);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            TreeNode tableNode = model.getNodeAtTableRow(row);

            int expectedValue = renderer.calculateNodeIndent(tableNode, table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());

            renderer.setNodeIndent(-1);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());

            renderer.setNodeIndent(-1);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());

            renderer.setNodeIndent(-1);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getNodeIndent());
        }
    }

    @Test
    public void testSetAdditionalPropertiesCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            renderer = new TestTreeCellRenderer(model);
            assertTrue(renderer.getBorder() instanceof TreeCellRenderer.ExpandHandleBorder);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);

            renderer.setBorder(BorderFactory.createEmptyBorder());
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);

            renderer.setBorder(BorderFactory.createEmptyBorder());
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);

            renderer.setBorder(BorderFactory.createEmptyBorder());
            renderer.getTableCellRendererComponent(table, description, false, true, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TestTreeCellRenderer(model);
        assertTrue(renderer.getBorder() instanceof TreeCellRenderer.ExpandHandleBorder);

        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);

            renderer.setBorder(BorderFactory.createEmptyBorder());
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);

            renderer.setBorder(BorderFactory.createEmptyBorder());
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);

            renderer.setBorder(BorderFactory.createEmptyBorder());
            renderer.getTableCellRendererComponent(table, description, false, true, row, 0);
            assertTrue(renderer.getBorder() instanceof BevelBorder);
        }
    }

    @Test
    public void testSetGetIconCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            renderer = new TreeCellRenderer(model);
            TreeNode tableNode = model.getNodeAtTableRow(row);
            Icon expectedValue = renderer.getNodeIcon(tableNode, table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getIcon());

            renderer.setIcon(renderer.collapsedIcon);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getIcon());

            renderer.setIcon(renderer.collapsedIcon);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getIcon());

            renderer.setIcon(renderer.collapsedIcon);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getIcon());
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + row;
            TreeNode tableNode = model.getNodeAtTableRow(row);
            Icon expectedValue = renderer.getNodeIcon(tableNode, table, description, false, false, row, 0);

            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getIcon());

            renderer.setIcon(renderer.collapsedIcon);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getIcon());

            renderer.setIcon(renderer.collapsedIcon);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getIcon());

            renderer.setIcon(renderer.collapsedIcon);
            renderer.getTableCellRendererComponent(table, expectedValue, false, true, row, 0);
            assertEquals(expectedValue, renderer.getIcon());
        }
    }

    @Test
    public void testSetGetForegroundColorsCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            renderer = new TreeCellRenderer(model);
            String description = "test" + Integer.toString(row);

            Color expectedValue = renderer.getSelectedForegroundColor(table, description, false, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getSelectedForegroundColor(table, description, false, row, 0));

            expectedValue = renderer.getSelectedForegroundColor(table, description, true, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getSelectedForegroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedForegroundColor(table, description, false, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getUnselectedForegroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedForegroundColor(table, description, true, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getUnselectedForegroundColor(table, description, false, row, 0));
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + Integer.toString(row);

            Color expectedValue = renderer.getSelectedForegroundColor(table, description, false, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getSelectedForegroundColor(table, description, false, row, 0));

            expectedValue = renderer.getSelectedForegroundColor(table, description, true, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getSelectedForegroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedForegroundColor(table, description, false, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getUnselectedForegroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedForegroundColor(table, description, true, row, 0);
            renderer.setForeground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, true, row, 0);
            assertEquals(expectedValue, renderer.getForeground());
            assertEquals(expectedValue, renderer.getUnselectedForegroundColor(table, description, true, row, 0));
        }
    }

    @Test
    public void testSetGetBackgroundColorsCorrectlyOnRender() {
        setUpRendererOnRandomTree();

        // Test with a new renderer for each row (setting from defaults):
        for (int row = 0; row < model.getRowCount(); row++) {
            renderer = new TreeCellRenderer(model);
            String description = "test" + Integer.toString(row);

            Color expectedValue = renderer.getSelectedBackgroundColor(table, description, false, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getSelectedBackgroundColor(table, description, false, row, 0));

            expectedValue = renderer.getSelectedBackgroundColor(table, description, true, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getSelectedBackgroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedBackgroundColor(table, description, false, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getUnselectedBackgroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedBackgroundColor(table, description, true, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, true, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getUnselectedBackgroundColor(table, description, true, row, 0));
        }

        // Test with the same renderer for each row (should still set correctly).
        renderer = new TreeCellRenderer(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            String description = "test" + Integer.toString(row);

            Color expectedValue = renderer.getSelectedBackgroundColor(table, description, false, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, false, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getSelectedBackgroundColor(table, description, false, row, 0));

            expectedValue = renderer.getSelectedBackgroundColor(table, description, true, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, true, true, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getSelectedBackgroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedBackgroundColor(table, description, false, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, false, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getUnselectedBackgroundColor(table, description, false, row, 0));

            expectedValue = renderer.getUnselectedBackgroundColor(table, description, true, row, 0);
            renderer.setBackground(Color.BLACK);
            renderer.getTableCellRendererComponent(table, description, false, true, row, 0);
            assertEquals(expectedValue, renderer.getBackground());
            assertEquals(expectedValue, renderer.getUnselectedBackgroundColor(table, description, true, row, 0));
        }
    }

    @Test
    public void testGetSetPixelsPerLevel() {
        renderer = new TreeCellRenderer(model);
        assertEquals(TreeCellRenderer.DEFAULT_PIXELS_PER_LEVEL, renderer.getPixelsPerLevel());

        renderer.setPixelsPerLevel(100);
        assertEquals(100, renderer.getPixelsPerLevel());
    }

    @Test
    public void testSetGetMaxIconWidth() {
        renderer = new TreeCellRenderer(model);
        Icon oldIcon = renderer.expandedIcon;

        int expectedValue = Math.max(renderer.expandedIcon.getIconWidth(), renderer.collapsedIcon.getIconWidth());
        assertEquals(expectedValue, renderer.maxIconWidth);

        renderer.setExpandedIcon(null);
        expectedValue = renderer.collapsedIcon.getIconWidth();
        assertEquals(expectedValue, renderer.maxIconWidth);

        renderer.setCollapsedIcon(null);
        expectedValue = 0;
        assertEquals(expectedValue, renderer.maxIconWidth);

        renderer.setExpandedIcon(oldIcon);
        expectedValue = oldIcon.getIconWidth();
        assertEquals(expectedValue, renderer.maxIconWidth);

        renderer.setCollapsedIcon(oldIcon);
        expectedValue = Math.max(renderer.expandedIcon.getIconWidth(), renderer.collapsedIcon.getIconWidth());
        assertEquals(expectedValue, renderer.maxIconWidth);
    }

    @Test
    public void testCalculateNodeIndent() {
        model = new TestTreeTableModel(rootNode, false);
        renderer = new TreeCellRenderer(model);
        int rootIndentHideRoot = renderer.calculateNodeIndent(rootNode);
        int childIndentHideRoot = renderer.calculateNodeIndent(child1);
        assertEquals(rootIndentHideRoot + renderer.getPixelsPerLevel(), childIndentHideRoot);

        model = new TestTreeTableModel(rootNode, true);
        renderer = new TreeCellRenderer(model);
        int rootIndentShowRoot = renderer.calculateNodeIndent(rootNode);
        int childIndentShowRoot = renderer.calculateNodeIndent(child1);
        assertEquals(rootIndentShowRoot + renderer.getPixelsPerLevel(), childIndentShowRoot);

        // child of hidden root is at same indentation of root if it's showing.
        assertEquals(rootIndentShowRoot, childIndentHideRoot);
    }

    @Test
    public void testClickOnExpand() {
        renderer = new TreeCellRenderer(model);
        TableColumnModel columnModel = model.getTableColumnModel();
        testClickOnExpand(columnModel);

        // Move the tree column one over to the right:
        columnModel.moveColumn(0, 1);
        testClickOnExpand(columnModel);

        columnModel.moveColumn(1, 2);
        testClickOnExpand(columnModel);
    }

    private void testClickOnExpand(TableColumnModel columnModel) {
        for (int colIndex = 0; colIndex < columnModel.getColumnCount(); colIndex++) {
            final TableColumn column = columnModel.getColumn(colIndex);
            final int columnStart = TreeUtils.calculateWidthToLeftOfColumn(columnModel, colIndex);

            // click will only happen if we're on a tree column:
            MouseEvent event = mouseEvent( columnStart + 8, 0);
            boolean expectedClick = column.getModelIndex() == 0; // the tree column always has model index of 0.
            assertEquals(expectedClick, renderer.clickOnExpand(rootNode, colIndex, event));

            // If the click happens outside of the node indent of the column, it will always be false, no matter what column it is.
            event = mouseEvent(columnStart + renderer.getNodeIndent(), 0);
            assertFalse(renderer.clickOnExpand(rootNode, colIndex, event));
        }
    }

    private MouseEvent mouseEvent(int x, int y) {
        return new MouseEvent(table, MouseEvent.MOUSE_RELEASED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void testToString() {
        renderer = new TreeCellRenderer(model);
        assertTrue(renderer.toString().contains(renderer.getClass().getSimpleName()));
    }

    protected static class TestTreeCellRenderer extends TreeCellRenderer {

        /**
         * Constructs a TreeCellRenderer given a TreeTableModel.
         *
         * @param treeTableModel The model to render cells for.
         * @throws IllegalArgumentException if the treeTableModel is null.
         */
        public TestTreeCellRenderer(TreeTableModel treeTableModel) {
            super(treeTableModel);
        }

        @Override
        protected void setAdditionalProperties(final TreeNode treeNode, final JTable table, final Object value,
                                               final boolean isSelected, final boolean hasFocus, final int row, final int column) {
             setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }
    }

    protected void setUpRendererOnRandomTree() {
        createRandomTree(0, true);
        model.bindTable(table);
        model.expandTree();
    }

}