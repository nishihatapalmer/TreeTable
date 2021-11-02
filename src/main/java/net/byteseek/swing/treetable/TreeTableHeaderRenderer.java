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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * Renders a header for a JTable which shows multi-column sorting, putting a number for each sort key against the
 * icon for ascending or descending.
 */
public class TreeTableHeaderRenderer extends JLabel implements TableCellRenderer {

    private static final int ICON_AND_NUMBER_WIDTH = 32; //TODO: hard coded value for ascend/descend icon and text... should calculate?
    private static final int VERTICAL_PADDING = 4; // white space above and below to raise header.

    private final SortIconBorder sortIconBorder;

    private Icon sortAscendingIcon;
    private Icon sortDescendingIcon;
    private int maxIconWidth;
    private JLabel sortAscendingIconLabel;
    private JLabel sortDescendingIconLabel;
    private Color sortcolumnTextColor;
    private Font headerFont;
    private Font boldHeaderFont;
    private boolean boldOnSorted;
    private Border focusHeaderBorder;
    private Border headerBorder;

    private int sortColumn;
    private SortOrder sortOrder;

    /**
     * Constructs a TreeTableHeaderRenderer.
     */
    public TreeTableHeaderRenderer() {
        sortIconBorder = new SortIconBorder();
        setSortAscendingIcon(UIManager.getIcon("Table.ascendingSortIcon"));
        setSortDescendingIcon(UIManager.getIcon("Table.descendingSortIcon"));
        setBorder(new EtchedBorder());
        focusHeaderBorder = UIManager.getBorder( "TableHeader.focusCellBorder");
        headerBorder = UIManager.getBorder("TableHeader.cellBorder");
        setSortColumnTextColor(Color.GRAY);
        setHorizontalAlignment(JLabel.CENTER);
        setBoldOnSorted(true);
    }

    @Override
    public void setBorder(final Border border) {
        // If the border is already a compound with an inner SortIconBorder, just set it directly.
        if (border instanceof CompoundBorder && ((CompoundBorder) border).getInsideBorder() == sortIconBorder) {
            super.setBorder(border);
        } else {  // Wrap the border in a CompoundBorder using the SortIconBorder inside to render sort icon and number.
            super.setBorder(new CompoundBorder(border, sortIconBorder));
        }
    }

    //TODO: configure whether number shows up in header for column sort.
    //      could have greyed out sort icons for later sort keys...?

    //TODO: provide standard getter / setting methods to allow override.
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setFont(header.getFont());
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                String displayText = value == null? "" : value.toString();
                setText(displayText);
                setToolTipText(displayText);
                setColumnSorted(table, column);
            }
        }
        return this;
    }

    @Override
    public void setFont(Font newFont) {
        if (newFont != headerFont || newFont != boldHeaderFont) {
            headerFont = newFont;
            boldHeaderFont = getBoldFont(newFont);
        }
        super.setFont(newFont);
    }

    public void setBoldOnSorted(boolean boldOnSorted) {
        this.boldOnSorted = boldOnSorted;
    }

    public boolean getBoldOnSorted() {
        return boldOnSorted;
    }

    public void setSortColumnTextColor(final Color sortcolumnTextColor) {
        this.sortcolumnTextColor = sortcolumnTextColor;
    }

    public Color getSortColumnTextColor() {
        return sortcolumnTextColor;
    }

    public void setSortAscendingIcon(final Icon sortAscendingIcon) {
        this.sortAscendingIcon = sortAscendingIcon;
        if (sortAscendingIconLabel == null) {
            sortAscendingIconLabel = new JLabel(sortAscendingIcon);
            sortAscendingIconLabel.setBorder(BorderFactory.createEmptyBorder());
        } else {
            sortAscendingIconLabel.setIcon(sortAscendingIcon);
        }
        setMaxIconWidth();
    }

    public Icon getSortAscendingIcon() {
        return sortAscendingIcon;
    }

    public void setSortDescendingIcon(final Icon sortDescendingIcon) {
        this.sortDescendingIcon = sortDescendingIcon;
        if (sortDescendingIconLabel == null) {
            sortDescendingIconLabel = new JLabel(sortDescendingIcon);
            sortDescendingIconLabel.setBorder(BorderFactory.createEmptyBorder());
        } else {
            sortDescendingIconLabel.setIcon(sortDescendingIcon);
        }
        setMaxIconWidth();
    }

    protected final void setMaxIconWidth() {
        int descendingWidth = sortDescendingIcon == null? 0 : sortDescendingIcon.getIconWidth();
        int ascendingWidth = sortAscendingIcon == null? 0 : sortAscendingIcon.getIconWidth();
        maxIconWidth = Math.max(descendingWidth, ascendingWidth);
    }

    public Icon getSortDescendingIcon() {
        return sortDescendingIcon;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    protected void setColumnSorted(JTable table, int column) {
        final RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
        if (rowSorter != null) {
            List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
            if (sortKeys != null) {
                final int columnModelIndex = table.convertColumnIndexToModel(column);
                for (int sortKeyIndex = 0; sortKeyIndex < sortKeys.size(); sortKeyIndex++) {
                    RowSorter.SortKey key = sortKeys.get(sortKeyIndex);
                    if (key.getColumn() == columnModelIndex && key.getSortOrder() != SortOrder.UNSORTED) {
                        sortOrder = key.getSortOrder();
                        sortColumn = sortKeyIndex;
                        sortIconBorder.insets.left = ICON_AND_NUMBER_WIDTH;
                        setBold(true);
                        return;
                    }
                }
            }
        }
        setBold(false);
        sortOrder = SortOrder.UNSORTED;
        sortColumn = -1;
        sortIconBorder.insets.left = 0;
    }

    protected void setBold(final boolean bold) {
        if (boldOnSorted) {
            setFont(bold ? boldHeaderFont : headerFont);
        }
    }

    protected Font getBoldFont(Font font) {
        if ((font.getStyle() & Font.BOLD) == Font.BOLD) {
            return font;
        }
        return font.deriveFont(font.getStyle() | Font.BOLD);
    }

    /**
     * A border that leaves space to paint a sort icon and sort key number.
     */
    protected class SortIconBorder implements Border {

        private final Insets insets = new Insets(VERTICAL_PADDING, ICON_AND_NUMBER_WIDTH, VERTICAL_PADDING, 0);

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (sortColumn >= 0 && sortOrder != SortOrder.UNSORTED) {
                final JLabel labelToPaint = sortOrder == SortOrder.ASCENDING ? sortAscendingIconLabel : sortDescendingIconLabel;
                labelToPaint.setSize(ICON_AND_NUMBER_WIDTH, height);
                labelToPaint.setFont(headerFont);
                labelToPaint.setText(Integer.toString(sortColumn + 1));
                labelToPaint.setForeground(sortcolumnTextColor);
                labelToPaint.paint(g);
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
