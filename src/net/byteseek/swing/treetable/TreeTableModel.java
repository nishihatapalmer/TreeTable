package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

//TOOO: bugs?
// * selection jumps when sorting - are events in right order?  This is looking better - test again.
// * header grid line is a bit off from cell grid lines.

//TODO: features:
// * show default folder / file icons for getAllowsChildren() ?
// * Different sort / click strategies: multi sort strategies... click column makes primary sort... no unsorted, etc.
// * current sort order always reversed everything - what if you want something to be independent, e.g. folders always on top?
//   sorts same ascending or descending...

//TODO: tests:
// * custom comparators
// * test customise visual appearance - all just on the JTable? Check we set all table settings in TreeTableCellRenderer.
// * test setting different icons.
// * test setting different keys for expand / collapse.

//TODO: functionality
// * show plus sign on nodes that we haven't dynamically expanded (if they support having children).
// * Should allow expand on a node with no children?
// * Should show expand handle for node with no children (what about dynamically adding nodes?)

/**
 * A tree table model which binds to a JTable as a TableModel given a root tree node.
 * It is abstract, as the implementor must provide a subclass providing the following:
 * <ul>
 *     <li>what the column definitions are</li>
 *     <li>whether any custom comparators or cell renderers will be supplied</li>
 * </ul>
 *
 * <p><b>Usage</b></p>
 * To use it, instantiate a subclassed TreeTableModel with a root node, then bind it to a JTable.
 * <p><b>Sorting</b>
 * If sorting using the {@link TreeTableRowSorter}, comparisons are performed like this:
 * <p>
 * <ol>
 *     <li>If you have defined a node comparator in your subclass, this will run first.
 *     <ul>
 *         <li>If the result is not equal, it's returned.  If it's equal we go to step 2.</li>
 *         <li>This lets you sort all children on a general characteristic first,
 *             e.g. are the nodes files or folders, before they are further sorted on column value.</li>
 *     </ul>
 *     </li>
 *     <li> For each of the SortKeys defined - try each comparator in turn:
 *     <ul>
 *         <li>Returning if the result is not equal.</li>
 *         <li>If a SortKey is set to UNSORTED, then the unsorted model ordering is returned.</li>
 *     </ul>
 *     <ol>
 *         <li>If you have defined a column comparator for the node values, this will be used.</li>
 *         <li>If no customer comparator, if the node values themselves are {@link Comparable}, they will be compared directly.</li>
 *         <li>If all else fails, compare on the string value of the objects.</li>
 *     </ol>
 *     </li>
 *     <li>If all comparisons were equal, it returns the unsorted model ordering.</li>
 * </ol>
 */
public abstract class TreeTableModel extends AbstractTableModel {

    /******************************************************************************************************************
     *                                         Constants
     */

    private static final char PLUS = '+';  // default node expand key char
    private static final char MINUS = '-'; // default node collapse key char

    /*
     * Immutable on construction
     */
    protected final int numColumns;
    protected final TreeTableNode rootNode;


    /******************************************************************************************************************
     *                                         Variables
     */

    /*
     * User modifiable properties of the model:
     */
    private boolean showRoot;
    private char expandChar = PLUS;
    private char collapseChar = MINUS;

    /*
     * Cached/calculated properties of the model:
     */
    private TableColumnModel columnModel;
    private TreeTableNodeList displayedNodes = new TreeTableNodeList();

    /*
     * Keyboard, mouse and tree events
     */
    private TreeKeyboardListener treeKeyboardListener;
    private Map<JTable, MouseListener> tableMouseListeners = new HashMap<>();
    private List<TreeTableEvent.Listener> eventListeners = new ArrayList<>(2);


    /******************************************************************************************************************
     *                                         Constructors
     */

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required and whether to show the root
     * of the tree.
     *
     * @param rootNode The root node of the tree to display.
     * @param numColumns The number of columns
     * @param showRoot Whether to show the root node of the tree.
     */
    public TreeTableModel(final TreeTableNode rootNode, final int numColumns, final boolean showRoot) {
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        if (!showRoot) { // ensure there is something to see if we aren't showing the root but it isn't currently expanded...
            rootNode.setExpanded(true);
        }
        this.numColumns = numColumns;
        this.treeKeyboardListener = new TreeKeyboardListener();
        buildVisibleNodes();
    }

    /******************************************************************************************************************
     *                        Methods to bind and unbind this model to and from a JTable
     */

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     */
    public void bindTable(final JTable table) {
        bindTable(table, new TreeTableRowSorter(this), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param rowSorter The row sorter to use with the table.
     */
    public void bindTable(final JTable table, final RowSorter<TreeTableModel> rowSorter) {
        bindTable(table, rowSorter, new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public void bindTable(final JTable table,  final TableCellRenderer headerRenderer) {
        bindTable(table, new TreeTableRowSorter(this), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param rowSorter The row sorter to use with the table.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public void bindTable(final JTable table,
                          final RowSorter<TreeTableModel> rowSorter,
                          final TableCellRenderer headerRenderer) {
        table.setAutoCreateColumnsFromModel(false);
        table.setModel(this);
        table.setColumnModel(getTableColumnModel());
        if (rowSorter != null) {
            table.setRowSorter(rowSorter);
        }
        table.addKeyListener(treeKeyboardListener);
        if (headerRenderer != null) {
            table.getTableHeader().setDefaultRenderer(headerRenderer);
        }
        addMouseListener(table);
    }

    /**
     * Unbinds a JTable from this model, and removes listeners and sorters.
     * It replaces the table and column model with a default one with no data.
     * Will do nothing if this object is not set to the table model of the JTable.
     *
     * @param table The JTable to unbind this model from.
     */
    public void unbindTable(final JTable table) {
        if (table.getModel() == this) {
            removeMouseListener(table);
            table.removeKeyListener(treeKeyboardListener);
            table.setRowSorter(null);
            table.setAutoCreateColumnsFromModel(true);
            table.setModel(new DefaultTableModel());
            //TODO: can we get a default header renderer and set it back?
        }
    }


    /******************************************************************************************************************
     *                              Abstract methods for subclasses to implement
     */

    /**
     * Returns the value of a node for a given column.
     * Subclasses should implement this to map a column to a node user object values.
     *
     * @param node The node to get a column value for.
     * @param column The column to get the value for.
     * @return The value of the column for the node.
     */
    public abstract Object getColumnValue(TreeTableNode node, int column);

    /**
     * Sets the value of the column to the node.
     *
     * @param node The node to set a column value for.
     * @param column The column to set the value for.
     * @param value The value to set.
     */
    public abstract void setColumnValue(TreeTableNode node, int column, Object value);

    /**
     * Returns a TableColumn defining the column.
     *
     * @param column The column index to get the TableColumn for.
     * @return a TableColumn defining the column.
     */
    public abstract TableColumn getTableColumn(int column);

    /**
     * Returns a Comparator for a given column index.
     * If null, then the model will compare node values directly if they implement Comparable,
     * or compare on the string value of the objects if they are not.
     *
     * @param column The column to return a Comparator for, or null if the default comparison is OK.
     * @return A Comparator for the given column, or null if no special comparator is required.
     */
    public abstract Comparator<?> getColumnComparator(int column);

    /**
     * Returns a Comparator for a node, or null if not set.
     * The node comparator (if set) is executed first, allowing comparisons to be made on the basis of the node itself,
     * no matter what column is being compared. If the result of the node comparator is equal, then the other comparators
     * are executed in turn.  For example, if you want to separate the sort of items based on whether they are folders
     * or files (to keep folders and files separate in the sort), you could implement a node comparator which sorts
     * on the basis of the node resource type.
     *
     * @return a Comparator for a node, or null if not set.
     */
    public abstract Comparator<TreeTableNode> getNodeComparator();

    public abstract Icon getNodeIcon(TreeTableNode node);

    /******************************************************************************************************************
     *                                    TableModel interface methods.
     */

    @Override
    public int getRowCount() {
        return displayedNodes.size();
    }

    @Override
    public int getColumnCount() {
        return numColumns;
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return getColumnValue(getNodeAtRow(row), column);
    }


    /******************************************************************************************************************
     *                                          Node getters
     */

    /**
     * Gets the node at the model row.
     *
     * @param modelIndex The row in the display model to get the node for.
     * @return The node for the row in the (unsorted) model index.
     */
    public TreeTableNode getNodeAtRow(final int modelIndex) {
        return modelIndex >= 0 && modelIndex < displayedNodes.size() ? displayedNodes.get(modelIndex) : null;
    }

    /**
     * Gets the node in a bound JTable given a table row index.
     * The view row index can differ from the rows in this underlying model if the table is sorted or filtered.
     *
     * @param table The JTable to get the node from at the row in the table.
     * @param tableIndex The row in the table to get the node from.
     * @return The node at the tableRow position in the JTable.
     */
    public TreeTableNode getNodeAtTableRow(final JTable table, final int tableIndex) {
        return getNodeAtRow(getModelIndex(table, tableIndex));
    }

    /**
     * Gets the index in the model of a row in a bound table.
     * If the table is unsorted, the model and table indexes will be identical.
     * If the table is sorted, then the row sorter can provide a mapping from the row in the visible table
     * to the row in this underlying model.
     *
     * @param table The JTable to get the model index for.
     * @param tableIndex The row in the JTable to get the model index for.
     * @return the model index of a row in a bound JTable.
     */
    public int getModelIndex(final JTable table, final int tableIndex) {
        final RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
        return rowSorter == null? tableIndex : rowSorter.convertRowIndexToModel(tableIndex);
    }


    /******************************************************************************************************************
     *                                          Root node visibility
     */

    /**
     * @return true if the root node will be displayed, false if not.
     */
    public boolean getShowRoot() {
        return showRoot;
    }

    /**
     * Sets whether the root node should be displayed or not, and updates the display model if the state changes.
     *
     * @param showRoot whether the root node should be displayed or not.
     */
    public void setShowRoot(final boolean showRoot) {
        if (this.showRoot != showRoot) {
            this.showRoot = showRoot;
            if (showRoot) {
                displayedNodes.insert(rootNode, 0);
                fireTableRowsInserted(0, 0);
            } else {
                displayedNodes.remove(0);
                fireTableRowsDeleted(0, 0);
            }
        }
    }


    /******************************************************************************************************************
     *                                           Event listeners
     */

    /**
     * Adds a listener to TreeTableEvents, which inform the listener when a node is about to expand or collapse.
     *
     * @param listener The listener to be notified of tree expand or collapse events.
     */
    public void addListener(final TreeTableEvent.Listener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes a TreeTableEvent listener.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(final TreeTableEvent.Listener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Adds a mouse listener for expand / collapse events to a JTable
     * and registers the listener in a map of JTable to MouseListeners.
     *
     * @param table The JTable to add the listener to.
     */
    public void addMouseListener(JTable table) {
        MouseListener listener = tableMouseListeners.get(table);
        if (listener == null) {
            listener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleExpansion(table, e);
                }
            };
            tableMouseListeners.put(table, listener);
            table.addMouseListener(listener);
        }
    }

    /**
     * Removes a mouse listener for expand / collapse events
     * from a JTable previously registered by a call to addMouseListener().
     *
     * @param table The JTable to remove registered listeners from.
     */
    public void removeMouseListener(JTable table) {
        MouseListener listener = tableMouseListeners.get(table);
        if (listener != null) {
            table.removeMouseListener(listener);
            tableMouseListeners.remove(table);
        }
    }


    /******************************************************************************************************************
     *                                          Column utility methods
     */

    public TableColumnModel getTableColumnModel() {
        if (columnModel == null) {
            columnModel = new DefaultTableColumnModel();
            for (int column = 0; column < getColumnCount(); column++) {
                columnModel.addColumn(getTableColumn(column));
            }
        }
        return columnModel;
    }

    public TableColumn createColumn(final String headerValue, final int modelIndex, final TableCellRenderer renderer) {
        TableColumn tableColumn = new TableColumn(modelIndex);
        tableColumn.setHeaderValue(headerValue);
        if (renderer != null) {
            tableColumn.setCellRenderer(renderer);
        }
        return tableColumn;
    }

    /******************************************************************************************************************
     *                                    Key bindings for expand and collapse.
     */

    /**
     * @return The char which if pressed will expand a node.
     */
    public char getExpandChar() {
        return expandChar;
    }

    /**
     * @return The char which if pressed will collapse a node.
     */
    public char getCollapseChar() {
        return collapseChar;
    }

    /**
     * @param expandChar Sets the char which if pressed will expand a node.
     */
    public void setExpandChar(char expandChar) {
        this.expandChar = expandChar;
    }

    /**
     * @param collapseChar Sets the char which if presssed will collapse a node.
     */
    public void setCollapseChar(char collapseChar) {
        this.collapseChar = collapseChar;
    }


    /******************************************************************************************************************
     *                                     Node expansion and collapse
     */

    protected void toggleExpansion(TreeTableNode node, int modelRow) {
        // Listeners may change the node structure (e.g. dynamically add, remove or change nodes).
        // If we're going to remove nodes (currently expanded), we need to get the number of nodes that are
        // *currently* visible, before any listeners run, so we know how many current nodes to remove, and
        // whether the node was already expanded or not.
        final boolean expanded = node.isExpanded();
        final int visibleChildrenBeforeListeners = expanded ? node.getChildVisibleNodeCount() : 0;
        if (listenersApprove(node)) {

            // If we had some visible nodes before toggling to unexpanded, remove those children.
            if (expanded) {
                removeDisplayedChildren(modelRow, visibleChildrenBeforeListeners);
            }
            // If we're still the same expansion as before listeners ran, toggle it.
            // If the listeners have already toggled it, but approved the event, we won't toggle it back.
            if (expanded == node.isExpanded()) {
                node.toggleExpanded();
            }

            // If we weren't already expanded before toggling to expanded, add the new children.
            if (!expanded) {
                addChildrenToDisplay(modelRow, node);
            }
        }
    }

    protected void toggleExpansion(final JTable table, final MouseEvent evt) {
        final Point point = evt.getPoint();
        final int tableRow = table.rowAtPoint(point);
        final int col = table.columnAtPoint(point); // does this change if columns are re-ordered?
        final int modelRow = getModelIndex(table, tableRow);
        final TreeTableNode node = displayedNodes.get(modelRow);
        if (clickOnExpand(node, col, evt)) {
            toggleExpansion(node, modelRow);
        }
    }

    protected boolean clickOnExpand(final TreeTableNode node, final int column, final MouseEvent evt) {
        final TableCellRenderer renderer = getTableColumnModel().getColumn(column).getCellRenderer();
        if (renderer instanceof TreeTableCellRenderer) {
            if (node != null && node.getAllowsChildren()) {
                final int columnStart = calculateWidthToLeft(column);
                final int expandEnd = columnStart + ((TreeTableCellRenderer) renderer).getNodeIndent(node);
                final int mouseX = evt.getPoint().x;
                if (mouseX > columnStart && mouseX < expandEnd) {
                    return true;
                }
            }
        }
        return false;
    }


    /******************************************************************************************************************
     *                                         Visible node management.
     */

    protected void buildVisibleNodes() {
        displayedNodes.clear();
        if (showRoot) {
            displayedNodes.add(rootNode);
        }
        rootNode.addVisibleChildren(displayedNodes);
    }

    protected boolean removeDisplayedChildren(final int row, final int numberToRemove) {
        if (numberToRemove > 0) {
            final int firstChildRow = row + 1;
            final int lastChildRow = firstChildRow + numberToRemove - 1;
            if (numberToRemove == 1) {
                displayedNodes.remove(firstChildRow);
            } else {
                displayedNodes.remove(firstChildRow, lastChildRow);
            }
            fireTableRowsDeleted(firstChildRow, lastChildRow);
            return true;
        }
        return false;
    }

    protected boolean addChildrenToDisplay(final int row, final TreeTableNode node) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            final int firstChildRow = row + 1;
            final List<TreeTableNode> newVisibleNodes = new ArrayList<>();
            node.addVisibleChildren(newVisibleNodes);
            if (newVisibleNodes.size() == 1) {
                displayedNodes.insert(newVisibleNodes.get(0), firstChildRow);
            } else {
                displayedNodes.insert(newVisibleNodes, firstChildRow);
            }
            final int lastChildRow = firstChildRow + newVisibleNodes.size() - 1;
            fireTableRowsInserted(firstChildRow, lastChildRow);
            return true;
        }
        return false;
    }

    /******************************************************************************************************************
     *                                            Listener management
     */

    private boolean listenersApprove(final TreeTableNode node) {
        final TreeTableEvent event = node.isExpanded() ?
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.COLLAPSING) :
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.EXPANDING);
        return listenersApprovedEvent(event);
    }

    private boolean listenersApprovedEvent(final TreeTableEvent event) {
        for (TreeTableEvent.Listener listener : eventListeners) {
            if (!listener.actionTreeEvent(event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A listener for keyboard events which expands or collapses a node if + or - is pressed on a node that allows children.
     */
    protected class TreeKeyboardListener implements KeyListener {

        @Override
        public void keyPressed(final KeyEvent e) {
            final Component component = e.getComponent();
            final char keyChar = e.getKeyChar();
            if ((keyChar == expandChar || keyChar == collapseChar) && component instanceof JTable) {
                final JTable table = (JTable) component;
                final int modelIndexRow = getModelIndex(table, table.getSelectedRow());
                final TreeTableNode node = getNodeAtRow(modelIndexRow);
                if (node.getAllowsChildren() && node.isExpanded() ? keyChar == collapseChar : keyChar == expandChar) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }

    /******************************************************************************************************************
     *                                              Miscellaneous
     */

    private int calculateWidthToLeft(final int colIndex) {
        TableColumnModel model = getTableColumnModel();
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += model.getColumn(col).getWidth();
        }
        return width;
    }

}
