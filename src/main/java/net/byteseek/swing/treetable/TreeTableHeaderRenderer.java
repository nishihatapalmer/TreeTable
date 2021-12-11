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

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
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

/**
 * Renders a header for a JTable which shows multi-column sorting,
 * painting a number for each sort key against the icon for ascending or descending
 * to show which is sorted first, second, or third.
 * <p>
 * It takes the font, foreground and background colors from the JTableHeader on the JTable.
 */
public class TreeTableHeaderRenderer extends JLabel implements TableCellRenderer {

    /* *****************************************************************************************************************
     *                                                Constants
     */

    /**
     * White space above and below to raise header height a bit.
     * Looks cramped if it is just the height of the text and icons.
     */
    protected static final int VERTICAL_PADDING = 4;

    /**
     * Padding to ensure sufficient room to paint text.
     * If you try to paint in something exactly the size of the text,
     * you get ellipses, it needs some margin.
     */
    protected static final int TEXT_PADDING = 8;

    /**
     * The inner border which contains the sort icons and sort order number
     * to the left of the header when a column is sorted, and has no
     * width if the column isn't sorted.
     */
    protected final SortIconBorder sortIconBorder = new SortIconBorder();

    /**
     * The JLabel used to paint the icon and sort order number.
     */
    protected final JLabel paintLabel;


    /* *****************************************************************************************************************
     *                                                Variables
     */

    /**
     * Whether to render the column header name in bold if the column is sorted.
     */
    protected boolean boldOnSorted;

    /**
     * Whether to show the sort order number or not.
     */
    protected boolean showNumber;

    /**
     * The icon for an ascending sort column.
     */
    protected Icon sortAscendingIcon;

    /**
     * The icon for a descending sort column.
     */
    protected Icon sortDescendingIcon;


    /* *****************************************************************************************************************
     *                          Cached or calculated properties of the TreeTableHeader.
     */

    /**
     * The last font set in the header on a call to getTableCellRendererComponent().
     * Cached in order that we can use it in the paintBorder() method of the SortIconBorder,
     * and also so we can determine if we need to generate a new Bold version of it if bold on sorted is selected.
     */
    protected Font cachedHeaderFont;

    /**
     * A bold version of the cached header font to use when rendering the sort column header if sorted.
     * If the cached header font is already bold, it will just be that font.
     */
    protected Font boldHeaderFont;

    /**
     * The number of the sort which is rendered with the icon, first column to be sorted, second, etc.
     */
    protected int sortOrderNumber;

    /**
     * The order of the current sort - ascending or descending (or unsorted, which will not be painted).
     */
    protected SortOrder sortOrder;

    /**
     * The maximum width of the ascending and descending icons,
     * calculated to ensure we consistently use the same space no
     * matter which one is showing (avoids jumping around visually).
     */
    protected int maxIconWidth;

    /**
     * The width of the sort number as text.  This is calculated from the font metrics when painting.
     */
    protected int sortNumberTextWidth;


    /* *****************************************************************************************************************
     *                                          Constructors
     */

    /**
     * Constructs a TreeTableHeaderRenderer, showing sort order numbers on columns, and column headers are bold on sort.
     */
    public TreeTableHeaderRenderer() {
        this(true);
    }

    /**
     * Constructs a TreeTableHeaderRenderer given whether to display the column number or not.
     * @param showNumber whether to display the sort order number or not on sorted columns.
     */
    public TreeTableHeaderRenderer(final boolean showNumber) {
        this(showNumber, true);
    }

    /**
     * Constructs a TreeTableHeaderRenderer given whether to display the column number or not.
     * @param showNumber whether to display the sort order number or not on sorted columns.
     * @param boldOnSorted whether the column header should be bold when it is sorted.
     */
    public TreeTableHeaderRenderer(final boolean showNumber, final boolean boldOnSorted) {
        setBorder(new EtchedBorder());
        paintLabel = new JLabel();
        paintLabel.setBorder(BorderFactory.createEmptyBorder());
        sortNumberTextWidth = 16; // start with 16 so we have something before a string is actually painted.
        setShowNumber(showNumber);
        setBoldOnSorted(boldOnSorted);
        setSortAscendingIcon(UIManager.getIcon("Table.ascendingSortIcon"));
        setSortDescendingIcon(UIManager.getIcon("Table.descendingSortIcon"));
        setHorizontalAlignment(JLabel.CENTER);
    }


    /* *****************************************************************************************************************
     *                                      TableCellRenderer main method
     */

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                String displayText = value == null? "" : value.toString();
                setText(displayText);
                setToolTipText(displayText);
                setColumnSortedProperties(table, column);
            }
        }
        return this;
    }


    /* *****************************************************************************************************************
     *                                            Getters and setters
     */

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ensures that whatever border is set, it is always wrapped in a CompoundBorder that
     * has the SortIconBorder field as its inner border.  This inner border paints the sort icon and number.
     */
    @Override
    public void setBorder(final Border border) {
        // If the border is already a compound with our inner SortIconBorder,
        // just set it directly to avoid repeatedly nesting compound borders.
        if (border instanceof CompoundBorder && ((CompoundBorder) border).getInsideBorder() == sortIconBorder) {
            super.setBorder(border);
        } else {  // Wrap the border in a CompoundBorder using the SortIconBorder inside to render sort icon and number.
            super.setBorder(new CompoundBorder(border, sortIconBorder));
        }
    }

    /**
     * Sets whether the column headers should be in a bold font if they are sorted.
     *
     * @param boldOnSorted whether the column headers should be in a bold font if they are sorted.
     */
    public void setBoldOnSorted(boolean boldOnSorted) {
        this.boldOnSorted = boldOnSorted;
    }

    /**
     * @return whether the column headers should be in a bold font if they are sorted.
     */
    public boolean getBoldOnSorted() {
        return boldOnSorted;
    }

    /**
     * @return Whether the sort order number of the column should be displayed.
     */
    public boolean getShowNumber() {
        return showNumber;
    }

    /**
     * Sets whether the sort order number of the column should be displayed.
     * @param showNumber whether the sort order number of the column should be displayed.
     */
    public void setShowNumber(final boolean showNumber) {
        this.showNumber = showNumber;
    }

    /**
     * Sets the icon for an ascending sort.
     * @param sortAscendingIcon the icon for an ascending sort.
     */
    public void setSortAscendingIcon(final Icon sortAscendingIcon) {
        this.sortAscendingIcon = sortAscendingIcon;
        setMaxIconWidth();
    }

    /**
     * @return the icon for an ascending sort.
     */
    public Icon getSortAscendingIcon() {
        return sortAscendingIcon;
    }

    /**
     * Sets the icon for a descending sort.
     * @param sortDescendingIcon the icon for a descending sort.
     */
    public void setSortDescendingIcon(final Icon sortDescendingIcon) {
        this.sortDescendingIcon = sortDescendingIcon;
        setMaxIconWidth();
    }

    /**
     * @return the icon for a descending sort.
     */
    public Icon getSortDescendingIcon() {
        return sortDescendingIcon;
    }

    /**
     * Mostly make this available for test purposes.
     * @return the inner border which is responsible for rendering the ascending / descending icons and column number.
     */
    protected Border getSortIconBorder() {
        return sortIconBorder;
    }

    /**
     * Mostly make this available for test purposes.
     * @return the JLabel that paints the icon and number.
     */
    protected JLabel getSortIconPainter() {
        return paintLabel;
    }

    /**
     * Sets the maximum icon width for the descending and ascending icons.
     */
    protected final void setMaxIconWidth() {
        int descendingWidth = sortDescendingIcon == null? 0 : sortDescendingIcon.getIconWidth();
        int ascendingWidth = sortAscendingIcon == null? 0 : sortAscendingIcon.getIconWidth();
        maxIconWidth = Math.max(descendingWidth, ascendingWidth);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(show sort number: " + showNumber + ')';
    }

    /**
     * Sets the properties for a column, depending on whether it is sorted or not.
     *
     * @param table The table we are rendering for.
     * @param column The column we need to set the sort properties for.
     */
    protected void setColumnSortedProperties(final JTable table, final int column) {
        final List<? extends RowSorter.SortKey> sortKeys = table.getRowSorter().getSortKeys();
        final int columnModelIndex = table.convertColumnIndexToModel(column);
        final int sortKeyIndex = TreeUtils.findSortKeyIndex(sortKeys, columnModelIndex);
        sortOrder = sortKeyIndex >= 0 ? sortKeys.get(sortKeyIndex).getSortOrder() : SortOrder.UNSORTED;
        if (sortOrder == SortOrder.UNSORTED) {
            setUnsortedColumnProperties(table);
        } else {
            setSortedColumnProperties(table, sortKeyIndex);
        }
    }

    /**
     * Sets the column properties for a sorted column.
     * @param table The table for the the unsorted column.
     * @param sortOrderNumber The sort number of the column.
     */
    protected void setSortedColumnProperties(final JTable table, final int sortOrderNumber) {
        this.sortOrderNumber = sortOrderNumber;
        sortIconBorder.insets.left = maxIconWidth + (showNumber ? sortNumberTextWidth : 0);
        setFont(getSortedColumnHeaderFont(table.getTableHeader().getFont()));
    }

    /**
     * Sets the column properties if the column is not sorted.
     * @param table The table for the the unsorted column.
     */
    protected void setUnsortedColumnProperties(final JTable table) {
        sortOrder = SortOrder.UNSORTED;
        sortOrderNumber = -1;
        sortIconBorder.insets.left = 0;
        setFont(table.getTableHeader().getFont());
    }

    /**
     * If boldOnSorted is true, then a bold font will be returned,
     * otherwise the standard JTableHeader font is returned.
     *
     * @param headerFont The font used by the JTableHeader.
     * @return the font to use for a sorted column header.
     */
    protected Font getSortedColumnHeaderFont(final Font headerFont) {
        if (boldOnSorted) {
            updateCachedFonts(headerFont);
            return boldHeaderFont;
        }
        return headerFont;
    }

    /**
     * Checks that our cached font is up to date.  If not, it caches the new font and a new bold
     * version of it to use on sorted columns.
     *
     * @param headerFont The font used by the JTableHeader.
     */
    protected void updateCachedFonts(final Font headerFont) {
        if (!headerFont.equals(cachedHeaderFont)) {
            cachedHeaderFont = headerFont;
            boldHeaderFont = headerFont.isBold() ? headerFont
                                                 : headerFont.deriveFont(headerFont.getStyle() | Font.BOLD);
        }
    }

    /**
     * A border that leaves space to paint a sort icon and sort key number.
     */
    protected class SortIconBorder implements Border {

        protected final Insets insets = new Insets(VERTICAL_PADDING, 0, VERTICAL_PADDING, 0);

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (sortOrderNumber >= 0 && sortOrder != SortOrder.UNSORTED) {
                if (showNumber) {
                    setNumber(g);
                } else {
                    setNoNumber();
                }
                final JLabel labelToPaint = paintLabel;
                labelToPaint.setIcon(sortOrder == SortOrder.ASCENDING ? sortAscendingIcon : sortDescendingIcon);
                labelToPaint.setSize(insets.left, height);
                labelToPaint.setFont(cachedHeaderFont);
                labelToPaint.setForeground(TreeTableHeaderRenderer.this.getForeground());
                labelToPaint.paint(g);
            }
        }

        /**
         * Sets the sort icon border with a sort order number.
         *
         * @param g the Graphics object which will be rendered on.
         */
        protected void setNumber(Graphics g) {
            final String sortNumber = Integer.toString(sortOrderNumber + 1);
            final FontMetrics metrics = g.getFontMetrics(cachedHeaderFont);
            sortNumberTextWidth = metrics.stringWidth(sortNumber) + TEXT_PADDING;
            insets.left = maxIconWidth + sortNumberTextWidth;
            paintLabel.setText(sortNumber);
        }

        /**
         * Sets the sort icon border with no sort number.
         */
        protected void setNoNumber() {
            sortNumberTextWidth = 0;
            insets.left = maxIconWidth;
            paintLabel.setText("");
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
