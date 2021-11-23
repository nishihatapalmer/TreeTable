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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TreeTableHeaderRendererTest {

    private JTable table;
    private TreeTableHeaderRenderer renderer;
    private JTableHeader header;
    private RowSorter<TableModel> sorter;
    private TableColumnModel tableColumnModel;
    private TableModel tableModel;


    @BeforeEach
    public void setup() {
        tableModel = new DefaultTableModel(10, 3);
        table = new JTable(tableModel);
        tableColumnModel = new DefaultTableColumnModel();
        tableColumnModel.addColumn(new TableColumn(0));
        tableColumnModel.addColumn(new TableColumn(1));
        tableColumnModel.addColumn(new TableColumn(3));
        table.setColumnModel(tableColumnModel);

        sorter = new TableRowSorter(tableModel);
        table.setRowSorter(sorter);
        renderer = new TreeTableHeaderRenderer();
        header = table.getTableHeader();
        header.setDefaultRenderer(renderer);
    }

    @Test
    public void testShowsNumberOnConstruction() {
        assertTrue(renderer.getShowNumber());
        assertTrue(renderer.toString().contains(renderer.getClass().getSimpleName()));
    }

    @Test
    public void testShowsNumberOnParameterizedConstruction() {
        renderer = new TreeTableHeaderRenderer(true);
        assertTrue(renderer.getShowNumber());

        renderer = new TreeTableHeaderRenderer(false);
        assertFalse(renderer.getShowNumber());
    }

    @Test
    public void testSettingBorderPreservesInnerSortIconBorder() {
        // Test default border is compound, Etched outside with sort Icon inside border.
        Border currentBorder = renderer.getBorder();
        assertTrue(currentBorder instanceof CompoundBorder);
        CompoundBorder border = (CompoundBorder) currentBorder;
        assertEquals(EtchedBorder.class, border.getOutsideBorder().getClass());
        assertEquals(TreeTableHeaderRenderer.SortIconBorder.class, border.getInsideBorder().getClass());
        TreeTableHeaderRenderer.SortIconBorder inner = (TreeTableHeaderRenderer.SortIconBorder) border.getInsideBorder();

        // Set a border to a different border:
        renderer.setBorder(new LineBorder(Color.BLACK));
        currentBorder = renderer.getBorder();

        // It's still a compound border with a new outside border, but the insider border is still the same inner sorticon border.
        assertTrue(currentBorder instanceof CompoundBorder);
        border = (CompoundBorder) currentBorder;
        assertEquals(LineBorder.class, border.getOutsideBorder().getClass());
        assertEquals(inner, border.getInsideBorder());

        // Set a compound border ourselves with the right inner border:
        border = new CompoundBorder(new BevelBorder(0), inner);
        renderer.setBorder(border);
        currentBorder = renderer.getBorder();

        // It's still a compound border with a new outside border,
        // but the insider border is still the same inner sorticon border.
        // It's not a compound border containing a nested compound border (we don't keep nesting them as we set them).
        assertTrue(currentBorder instanceof CompoundBorder);
        border = (CompoundBorder) currentBorder;
        assertEquals(BevelBorder.class, border.getOutsideBorder().getClass());
        assertEquals(inner, border.getInsideBorder());

        // Set a compound border that has the wrong inner border:
        border = new CompoundBorder(new BevelBorder(0), new EtchedBorder());
        renderer.setBorder(border);
        currentBorder = renderer.getBorder();

        // It's still a compound border with a new outside border,
        // but the insider border is still the same inner sorticon border.
        // It's not a compound border containing a nested compound border (we don't keep nesting them as we set them).
        assertTrue(currentBorder instanceof CompoundBorder);
        border = (CompoundBorder) currentBorder;
        assertEquals(CompoundBorder.class, border.getOutsideBorder().getClass());
        border = (CompoundBorder) border.getOutsideBorder();
        assertEquals(BevelBorder.class, border.getOutsideBorder().getClass());
        assertEquals(EtchedBorder.class, border.getInsideBorder().getClass());
        assertEquals(inner, ((CompoundBorder) currentBorder).getInsideBorder());
    }

    @Test
    public void testGetTableCellComponentSetTextToValue() {
        String expected = "value";
        renderer.getTableCellRendererComponent(table, expected, false, false, 0, 0);
        assertEquals(expected, renderer.getText());

        expected = "something else";
        renderer.getTableCellRendererComponent(table, expected, false, false, 0, 0);
        assertEquals(expected, renderer.getText());
    }

    @Test
    public void testGetTableCellComponentSetTootipTextToValue() {
        String expected = "value";
        renderer.getTableCellRendererComponent(table, expected, false, false, 0, 0);
        assertEquals(expected, renderer.getToolTipText());

        expected = "something else";
        renderer.getTableCellRendererComponent(table, expected, false, false, 0, 0);
        assertEquals(expected, renderer.getToolTipText());
    }

    @Test
    public void testGetTableCellComponentSetsColorsFromTableHeader() {
        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);

        assertEquals(header.getForeground(), renderer.getForeground());
        assertEquals(header.getBackground(), renderer.getBackground());

        header.setForeground(Color.CYAN);
        header.setBackground(Color.MAGENTA);
        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);

        assertEquals(Color.CYAN, renderer.getForeground());
        assertEquals(Color.MAGENTA, renderer.getBackground());
    }

    @Test
    public void testGetTableCellComponentSetsFontFromTableHeader() {
        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        assertEquals(header.getFont(), renderer.getFont());

        Font expected = new Font(null, 0, 0);
        header.setFont(expected);

        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        assertEquals(expected, renderer.getFont());
    }

    @Test
    public void testBoldFontOnSortedColumn() {
        RowSorter.SortKey sortKey = new RowSorter.SortKey(0, SortOrder.ASCENDING);
        List<RowSorter.SortKey> keyList = new ArrayList<>();
        keyList.add(sortKey);
        sorter.setSortKeys(keyList);

        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        assertNotEquals(header.getFont(), renderer.getFont());
        assertTrue((renderer.getFont().getStyle() & Font.BOLD) == Font.BOLD);

        Font expected = new Font(null, 0, 0);
        header.setFont(expected);

        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        assertNotEquals(header.getFont(), renderer.getFont());
        assertTrue((renderer.getFont().getStyle() & Font.BOLD) == Font.BOLD);
    }

    @Test
    public void testNotBoldFontOnSortedColumnIfBoldNotSet() {
        setColumnOneSorted();

        renderer.setBoldOnSorted(false);

        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        assertEquals(header.getFont(), renderer.getFont());

        Font expected = new Font(null, 0, 0);
        header.setFont(expected);

        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        assertEquals(expected, renderer.getFont());
    }

    private void setColumnOneSorted() {
        RowSorter.SortKey sortKey = new RowSorter.SortKey(0, SortOrder.ASCENDING);
        List<RowSorter.SortKey> keyList = new ArrayList<>();
        keyList.add(sortKey);
        sorter.setSortKeys(keyList);
    }

    @Test
    public void testGetSetBoldOnSorted() {
        renderer.setBoldOnSorted(false);
        assertFalse(renderer.getBoldOnSorted());

        renderer.setBoldOnSorted(true);
        assertTrue(renderer.getBoldOnSorted());
    }

    @Test
    public void testGetSetShowNumber() {
        renderer.setShowNumber(false);
        assertFalse(renderer.getShowNumber());

        renderer.setShowNumber(true);
        assertTrue(renderer.getShowNumber());
    }

    @Test
    public void getGetSetSortIcons() {
        Icon ascending = renderer.getSortAscendingIcon();
        Icon descending = renderer.getSortDescendingIcon();

        renderer.setSortAscendingIcon(descending);
        assertEquals(descending, renderer.getSortAscendingIcon());
        assertEquals(descending, renderer.getSortDescendingIcon());

        renderer.setSortDescendingIcon(ascending);
        assertEquals(descending, renderer.getSortAscendingIcon());
        assertEquals(ascending, renderer.getSortDescendingIcon());
    }


    @Test
    public void testPaintSortIconBorderSortedColumnShowNumber() {
        setColumnOneSorted();
        renderer.setShowNumber(true);
        testPaintSortedColumn(createMockGraphics(), true);
    }

    @Test
    public void testPaintSortIconBorderSortedColumnHideNumber() {
        setColumnOneSorted();
        renderer.setShowNumber(false);
        testPaintSortedColumn(createMockGraphics(), false);
    }

    private void testPaintSortedColumn(Graphics graphics, boolean showNumber) {
        Border border = renderer.getSortIconBorder();
        JLabel painter = renderer.getSortIconPainter();

        assertNull(painter.getIcon());
        Dimension size = painter.getSize();
        assertEquals(0, size.width);

        // set properties for row 0 - rootNode and paint the border:
        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        border.paintBorder(table, graphics, 0, 0, 128, 32);

        // Test properties for node are now set on the label.
        assertEquals(table.getTableHeader().getFont(), painter.getFont());
        assertEquals(border.getBorderInsets(table).left, painter.getSize().getWidth());
        assertEquals(showNumber ? "1" : "", painter.getText());

        // Test bold font when it is already a bold font:
        Font headerFont = table.getTableHeader().getFont();
        Font boldFont = headerFont.deriveFont(headerFont.getStyle() | Font.BOLD);
        table.getTableHeader().setFont(boldFont);

        // set properties for row 0 - rootNode and paint the border:
        renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);
        border.paintBorder(table, graphics, 0, 0, 128, 32);

        // Test properties for node are now set on the label.
        assertEquals(boldFont, painter.getFont());
        assertEquals(border.getBorderInsets(table).left, painter.getSize().getWidth());
        assertEquals(showNumber ? "1" : "", painter.getText());
    }

    private Graphics createMockGraphics() {
        final FontMetrics fontMetrics = mock(FontMetrics.class);
        when(fontMetrics.stringWidth(any())).thenReturn(67);

        final Graphics graphics = mock(Graphics.class);
        when(graphics.create()).thenReturn(graphics);
        when(graphics.getFont()).thenReturn(table.getTableHeader().getFont());
        when(graphics.getFontMetrics(any())).thenReturn(fontMetrics);

        return graphics;
    }

    @Test
    public void testSortIconBorderIsNotOpaque() {
        assertFalse(renderer.sortIconBorder.isBorderOpaque());
    }

}