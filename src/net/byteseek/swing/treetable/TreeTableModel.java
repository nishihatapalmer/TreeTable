package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

//TODO: tool tips - some display, some don't.  Investigate what we want to do here and if it meet's DROID requirements.

//TODO: key bindings (only have mouse click for expand/collapse)



//TODO: test dynamic expand remove nodes

//TODO: customise visual appearance - all just on the JTable? Check we set all table settings in TreeTableCellRenderer.
//TODO: test setting different icons.
//TODO: table header - change display of sorted columns (bold, multi-column).

//TODO: sorting multi column sort header and behaviour?

//TODO: show plus sign on nodes that we haven't dynamically expanded (if they support having children).
//TODO: Should allow expand on a node with no children?
//TODO: Should show expand handle for node with no children (what about dynamically adding nodes?)

/**
 * A tree table model which implements the binding to a JTable as a TableModel given a root tree node.
 * It is abstract, as the implementor must customise (1) how many columns exist (2) what the columns are
 * and (3) whether any custom comparators or cell renderers will be supplied.
 *
 * To use it, instantiate a TreeTableModel with a root node, then bind it to a JTable.
 */
public abstract class TreeTableModel extends AbstractTableModel {

    private final int numColumns;
    private final TreeTableNode rootNode;

    private boolean showRoot;
    private TableColumnModel columnModel;
    private TreeTableNodeList displayedNodes = new TreeTableNodeList();
    private List<TreeTableEvent.Listener> eventListeners = new ArrayList<>(2);
    private TreeKeyboardListener treeKeyboardListener;
    private Map<JTable, MouseListener> tableMouseListeners = new HashMap<>();

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
        if (!showRoot) {
            rootNode.setExpanded(true);
        }
        this.numColumns = numColumns;
        this.treeKeyboardListener = new TreeKeyboardListener();
        buildVisibleNodes();
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keybaord events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     */
    public void bindTable(final JTable table) {
        table.setAutoCreateColumnsFromModel(false);
        table.setModel(this);
        table.setColumnModel(getTableColumnModel());
        final RowSorter<TreeTableModel> rowSorter = new TreeTableRowSorter(this);
        table.setRowSorter(rowSorter);
        table.addKeyListener(treeKeyboardListener);
        addMouseListener(table);
    }

    /**
     * Unbinds a JTable from this model, and removes listeners, sorters and columns, replacing all with a
     * default table model and a default column model.  Will do nothing if this object is not set to the table model
     * of the JTable.
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
        }
    }

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

    /**
     * @return true if the root node will be displayed, false if not.
     */
    public boolean getShowRoot() {
        return showRoot;
    }

    /**
     * Sets whether the root node should be displayed or not.
     *
     * @param showRoot whether the root node should be displayed or not.
     */
    public void setShowRoot(boolean showRoot) {
        if (this.showRoot != showRoot) {
            this.showRoot = showRoot;
            fireTableDataChanged();
        }
    }

    public TreeTableNode getNodeAtRow(final int row) {
        return row >= 0 && row < displayedNodes.size() ? displayedNodes.get(row) : null;
    }

    public int getIndexAtTableRow(final JTable table, final int tableRow) {
        final RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
        return rowSorter == null? tableRow : rowSorter.convertRowIndexToModel(tableRow);
    }

    public TreeTableNode getNodeAtTableRow(final JTable table, final int tableRow) {
        return displayedNodes.get(getIndexAtTableRow(table, tableRow));
    }

    public TableColumnModel getTableColumnModel() {
        if (columnModel == null) {
            columnModel = new DefaultTableColumnModel();
            for (int column = 0; column < getColumnCount(); column++) {
                columnModel.addColumn(getTableColumn(column));
            }
        }
        return columnModel;
    }

    protected TableColumn createColumn(final String headerValue, final int modelIndex, final TableCellRenderer renderer) {
        TableColumn tableColumn = new TableColumn(modelIndex);
        tableColumn.setHeaderValue(headerValue);
        if (renderer != null) {
            tableColumn.setCellRenderer(renderer);
        }
        return tableColumn;
    }

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
            // If the listeners have already toggled it, we won't toggle it back.
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
        final int modelRow = getIndexAtTableRow(table, tableRow);
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

    private boolean listenersApprove(final TreeTableNode node) {
        final TreeTableEvent event = node.isExpanded() ?
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.COLLAPSING) :
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.EXPANDING);
        return listenersApprovedEvent(event);
    }



    private int calculateWidthToLeft(final int colIndex) {
        TableColumnModel model = getTableColumnModel();
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += model.getColumn(col).getWidth();
        }
        return width;
    }

    private boolean removeDisplayedChildren(final int row, final int numberToRemove) {
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

    private boolean addChildrenToDisplay(final int row, final TreeTableNode node) {
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

    private void buildVisibleNodes() {
        displayedNodes.clear();
        if (showRoot) {
            displayedNodes.add(rootNode);
        }
        rootNode.addVisibleChildren(displayedNodes);
    }

    private boolean listenersApprovedEvent(final TreeTableEvent event) {
        for (TreeTableEvent.Listener listener : eventListeners) {
            if (!listener.actionTreeEvent(event)) {
                return false;
            }
        }
        return true;
    }

    private void addMouseListener(JTable table) {
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

    private void removeMouseListener(JTable table) {
        MouseListener listener = tableMouseListeners.get(table);
        if (listener != null) {
            table.removeMouseListener(listener);
            tableMouseListeners.remove(table);
        }
    }

    /**
     * A listener for keyboard events which expands or collapses a node if + or - is pressed on a node that allows children.
     */
    private class TreeKeyboardListener implements KeyListener {

        private static final char PLUS = '+';
        private static final char MINUS = '-';

        @Override
        public void keyPressed(final KeyEvent e) {
            final char keyChar = e.getKeyChar();
            final Component component = e.getComponent();
            if ((keyChar == PLUS || keyChar == MINUS) && component instanceof JTable) {
                final JTable table = (JTable) component;
                final int selectedRow = table.getSelectedRow();
                final TreeTableNode node = getNodeAtTableRow(table, selectedRow);
                if (node.getAllowsChildren()) {
                    toggleExpansion(node, selectedRow);
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

}
