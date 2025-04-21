package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public final class TableUtils {

    /**
     * Default visible width of a TableColumn created using the utility createColumn() methods, if you don't specify a width.
     */
    private static final int DEFAULT_COLUMN_WIDTH = 75;

    private TableUtils() {}

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell renderer or cell editor specified - JTable has default renderers and editors for simple data types.
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @return a TableColumn with the values provided and defaults for the others.
     */
    public static TableColumn createColumn(final int modelIndex, final Object headerValue) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, null, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell renderer or cell editor specified - JTable has default renderers and editors for simple data types.
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param width The width of the column.
     * @return a TableColumn with the values provided and defaults for the others.
     */
    public static TableColumn createColumn(final int modelIndex, final Object headerValue, final int width) {
        return createColumn(modelIndex, headerValue, width, null, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell editor specified - JTable has default editors for simple data types.
     * If specifying a cell renderer for model index 0, it must be capable of rendering the tree structure.
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @return a TableColumn with the values provided and defaults for the others.
     */
    public static TableColumn createColumn(final int modelIndex, final Object headerValue,
                                       final TableCellRenderer cellRenderer) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, cellRenderer, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell editor specified - JTable has default editors for simple data types.
     * If specifying a cell renderer for model index 0, it must be capable of rendering the tree structure.     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param width The width of the column.
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @return a TableColumn with the values provided and defaults for the others.
     */
    public static TableColumn createColumn(final int modelIndex, final Object headerValue, final int width,
                                       final TableCellRenderer cellRenderer) {
        return createColumn(modelIndex, headerValue, width, cellRenderer, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @param cellEditor The TableCellEditor to use with the data types in that column.
     * @return a TableColumn with the values provided, and a default width.
     */
    public static TableColumn createColumn(final int modelIndex, final Object headerValue,
                                       final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, cellRenderer, cellEditor);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param width The width of the column.
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @param cellEditor The TableCellEditor to use with the data types in that column.
     * @return a TableColumn with the values provided.
     */
    public static  TableColumn createColumn(final int modelIndex,  final Object headerValue, final int width,
                                       final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
        final TableColumn column = new TableColumn(modelIndex, width, cellRenderer, cellEditor);
        column.setHeaderValue(headerValue);
        return column;
    }

    /**
     * Builds a TableColumnModel instance based on the provided list of column headers.
     *
     * @param headers the list of headers to be used for creating the columns
     * @return a TableColumnModel containing columns corresponding to the provided headers
     */
    public static TableColumnModel buildTableColumnModel(final List<?> headers) {
        TableColumnModel columnModel = new DefaultTableColumnModel();
        int index = 0;
        for (Object header : headers) {
            columnModel.addColumn(createColumn(index++, header));
        }
        return columnModel;
    }

    /**
     * Calculates the space taken up by columns to the left of a column in the TableColumnModel.
     *
     * @param columnModel The table column model
     * @param colIndex The column index of the column in the table column model (not the model index of the column).
     * @return the space taken up by columns to the left of the column with colIndex in the TableColumnModel.
     */
    public static int calculateWidthToLeftOfColumn(final TableColumnModel columnModel, final int colIndex) {
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += columnModel.getColumn(col).getWidth();
        }
        return width;
    }

    /**
     * Returns the TableColumn in the TableColumnModel with the given model index, or null if no such column exists.
     * The column index of a column may not match the model index if they have been re-ordered.
     * @param columnModel The TableColumnModel to search.
     * @param modelIndex The model index of the TableColumn requested.
     * @return the TableColumn in the TableColumnModel with the given model index, or null if no such column exists.
     */
    public static TableColumn getColumnWithModelIndex(final TableColumnModel columnModel, final int modelIndex) {
        for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
            final TableColumn column = columnModel.getColumn(columnIndex);
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
        return null;
    }

    /**
     * @param sortKeys The sort keys to check.
     * @param modelIndex The model index of the column to check.
     * @return true if a column with the column model index exists in the list of sort keys.
     */
    public static boolean columnInSortKeys(final List<? extends RowSorter.SortKey> sortKeys, final int modelIndex) {
        return findSortKeyIndex(sortKeys, modelIndex) >= 0;
    }

    /**
     * Returns the index of the sort key for a column in a table, or -1 if that column doesn't have a sort key for it.
     * @param sortKeys The list of sort keys to look in.
     * @param modelIndex The model index of the column to find a sort key for.
     * @return The index of the sort key for a column, or -1 if that column doesn't have a sort key for it.
     */
    public static int findSortKeyIndex(final List<? extends RowSorter.SortKey> sortKeys, final int modelIndex) {
        if (sortKeys != null) {
            for (int sortKeyIndex = 0; sortKeyIndex < sortKeys.size(); sortKeyIndex++) {
                RowSorter.SortKey key = sortKeys.get(sortKeyIndex);
                if (key.getColumn() == modelIndex) {
                    return sortKeyIndex;
                }
            }
        }
        return -1;
    }

    /**
     * Toggles the sort order of a JTable column, if the table is sorted.
     *
     * @param table The table the column is in.
     * @param columnIndex The column index to toggle the sort state of.
     */
    public static void toggleSortColumn(JTable table, int columnIndex) {
        if (columnIndex >= 0) {
            RowSorter<?> sorter = table == null? null : table.getRowSorter();
            if (sorter != null) {
                sorter.toggleSortOrder(table.convertColumnIndexToModel(columnIndex));
            }
        }
    }

    /**
     * Resizes a table column to fit the contents, but no smaller than the column min width or larger
     * than the column max width.
     *
     * @param table The table the column is in.
     * @param columnIndex The index of the column to resize to fit contents.
     */
    public static void resizeColumnToFitContents(JTable table, int columnIndex) {
        if (columnIndex >= 0 && table != null) {
            TableColumn tableColumn = table.getColumnModel().getColumn(columnIndex);
            tableColumn.setPreferredWidth(calculateFitToContentWidth(table, columnIndex));
            table.doLayout();
        }
    }

    /**
     * Calculates the width of a column, if it is resized to fit the contents of the rows in the table.
     * <p>
     * It takes into account the header width as well as the data in the rows and the table intercell spacing.
     * The width returned will be between the column min Width and the column maxWidth.
     *
     * @param table The table the column is in.
     * @param columnIndex The index of the column.
     * @return The preferred width of the column sized to fit its contents, between the min and max widths defined for the column.
     */
    public static int calculateFitToContentWidth(JTable table, int columnIndex) {
        int fitWidth = 0;
        if (table != null && columnIndex >= 0) {
            TableColumn column = table.getColumnModel().getColumn(columnIndex);

            // Get the table header column width:
            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, columnIndex);
            fitWidth = Math.max(column.getMinWidth(), headerComp.getPreferredSize().width);

            // Find any bigger preferred sizes given the data in the rows:
            int maxColumnWidth = column.getMaxWidth();
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, columnIndex);
                Component c = table.prepareRenderer(cellRenderer, row, columnIndex);
                fitWidth = Math.max(fitWidth, c.getPreferredSize().width + table.getIntercellSpacing().width);

                if (fitWidth >= maxColumnWidth) {
                    fitWidth = maxColumnWidth;
                    break;
                }
            }
        }
        return fitWidth;
    }

    public static boolean isResizeCursor(Cursor cursor) {
        int cursorType = cursor.getType();
        return cursorType == Cursor.E_RESIZE_CURSOR || cursorType == Cursor.W_RESIZE_CURSOR;
    }

    /**
     * A class that implements a double-click listener for JTableHeaders to toggle sorting.
     * <p>
     * The built-in mouse listener used by default in many Look and Feels in Swing descends from
     * the listener defined in the {@link javax.swing.plaf.basic.BasicTableHeaderUI} class.
     * This explicitly does not respond to double-clicks - or quadruple clicks - or any even number of clicks.
     * It will only trigger a sort when the click == 1, or click == 3 - all the odd clicks.
     * <p>
     * This listener plugs that gap, by also triggering a sort on the even clicks.  Adding this
     * listener to the table header component will cause the header to sort on all clicks.  It will
     * then respond to the odd clicks with the original listener, and to the even ones using this one.
     * <p>
     * <pre>
     *   JTableHeader header = table.getTableHeader();
     *   header.addMouseListener(new TableUtils.HeaderDoubleClickSortMouseListener(header));
     * </pre>
     *
     */
    public static class HeaderSortMouseDoubleClickListener extends MouseAdapter {

        private final JTableHeader header;

        public HeaderSortMouseDoubleClickListener(JTableHeader header) {
            this.header = header;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() %2 == 0 && SwingUtilities.isLeftMouseButton(e) && header.isEnabled()) {
                toggleSortColumn(header.getTable(), header.columnAtPoint(e.getPoint()));
            }
        }

    }

    /**
     * A class that implements a mouse click listener for JTableHeader components that will sort on every left
     * mouse button click, unless the cursor is a resize cursor.  If a resize cursor, then a double click
     * will resize the column to fit the contents of the rows.
     */
    public static class HeaderSortResizeMouseClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader header = e.getSource() instanceof JTableHeader ? (JTableHeader) e.getSource() : null;
                if (header != null && header.isEnabled()) {
                    if (!isResizeCursor(header.getCursor())) {
                        toggleSortColumn(header.getTable(), header.columnAtPoint(e.getPoint()));
                    } else if (e.getClickCount() == 2) {
                        resizeColumnToFitContents(header.getTable(), header.columnAtPoint(e.getPoint()));
                    }
                }
            }
        }
    }


    /**
     * A class which intercepts AWTEvents before they are dispatched to any registered listeners for a component
     * in order to replace the listeners for a particular type of mouse event.
     * <p>
     * This allows us to override default behaviour inside various Swing Look and Feels for one type of mouse event,
     * while preserving all the other behaviours.
     * <p>
     * There can be a small performance impact in monitoring all AWTEvents globally, but used sparingly this allows
     * a clean way to change default behaviour without re-implementing all mouse behaviours associated with a look
     * and feel.
     * <p>
     * It also registers a HierarchyListener to automatically unregister the AWTEvent listener
     * if the component it is monitoring becomes un-displayable.
     * This listener is also removed if the AWTEventListener is manually removed.
     */
    public static class AWTMouseListenerReplacer implements AWTEventListener {

        /**
         * The component to monitor AWTEvents for.
         */
        protected JComponent component;

        /**
         * The id of the mouse event we want to replace.
         */
        protected final int eventReplacementId;

        /**
         * The mouse listener which implements the new behaviour.
         * If null, then no replacement listener will be invoked, but the original listener for that event will not run.
         */
        protected final MouseListener replacementListener;

        /**
         * A HierarchyListener which watches to see if the component we are monitoring becomes
         * un-displayable, and unregisters this class and itself as listeners if so.
         */
        protected HierarchyListener removeListener = new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                    if (!component.isDisplayable()) {
                        deactivate();
                    }
                }
            }
        };

        /**
         * Constructs an AWTMouseListenerReplacer.  You must call activate() after construction to make it active.
         *
         * @param replacementListener The mouse listener we want to replace the default behaviour with.
         * @param eventReplacementId The id of the mouse event we want to replace.
         * @param component The component to monitor mouse events for.
         */
        public AWTMouseListenerReplacer(MouseListener replacementListener, int eventReplacementId, JComponent component) {
            this.eventReplacementId = eventReplacementId;
            this.replacementListener = replacementListener;
            this.component = component;
        }

        /**
         * Makes the replacement listener active, intercepting AWTEvents.
         */
        public void activate() {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
            component.addHierarchyListener(removeListener);
        }

        /**
         * Makes the replacement listener inactive, no longer intercepting AWTEvents.
         */
        public void deactivate() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
            component.removeHierarchyListener(removeListener);
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            if (event.getSource() == component && event.getID() == eventReplacementId && event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                if (shouldReplace(mouseEvent)) {
                    handleWithReplacement(mouseEvent);
                    mouseEvent.consume(); // prevent any other mouse listeners from consuming this event.
                }
            }
        }

        /**
         * A method which subclasses can use to implement additional logic to determine if the event should be
         * replaced by the new listener.  This class just returns true.
         *
         * @param event The event which might be replaced.
         * @return true if the event should be replaced.
         */
        protected boolean shouldReplace(MouseEvent event) {
            return true;
        }

        /**
         * Dispatches the mouse event to the new replacement listener.
         *
         * @param event The event for the replacement listener to process.
         */
        protected void handleWithReplacement(MouseEvent event) {
            if (replacementListener != null) {
                switch (event.getID()) {
                    case MouseEvent.MOUSE_PRESSED:
                        replacementListener.mousePressed(event);
                        break;
                    case MouseEvent.MOUSE_RELEASED:
                        replacementListener.mouseReleased(event);
                        break;
                    case MouseEvent.MOUSE_CLICKED:
                        replacementListener.mouseClicked(event);
                        break;
                    case MouseEvent.MOUSE_EXITED:
                        replacementListener.mouseExited(event);
                        break;
                    case MouseEvent.MOUSE_ENTERED:
                        replacementListener.mouseEntered(event);
                        break;
                }
            }
        }
    }


}
