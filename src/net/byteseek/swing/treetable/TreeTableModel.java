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

import net.byteseek.utils.collections.BlockModifyArrayList;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

//TODO: multi selection - expand/collapse/toggle instead of only first selection.

//TODO: refresh nodes

//TODO: tree model listener methods.

//TODO: explain using tree models to update the tree, or refresh() calls if you don't want to do it that way.

//TODO: profile large trees, sorting, inserting/removing nodes dynamically, etc.

/**
 * A tree table model which binds to a JTable as a TableModel given a root tree node.
 * It is abstract, as the implementor must provide a subclass implementing the following:
 * <ul>
 *     <li>what the column definitions are</li>
 *     <li>getter that maps columns to fields.</li>
 * </ul>
 *
 * <p><b>Usage</b></p>
 * To use it, instantiate a subclassed TreeTableModel with a root TreeNode, then bind it to a JTable.
 */
public abstract class TreeTableModel extends AbstractTableModel implements TreeModelListener {

    /**
     * Convenience static utility method to build a root MutableTreeNode and all sub children given the user object
     * which is the parent, and a ChildProvider which returns the list of children for user objects in the tree.
     * This can often be provided as one line lambda expression.
     *
     * @param parent The user object which is at the root of the tree.
     * @param provider An object which provides a list of user objects from the parent user object.
     * @return A DefaultMutableTreeNode with all child nodes built and associated with their corresponding user objects.
     */
    public static final MutableTreeNode buildTree(final Object parent, final TreeTableModel.ChildProvider provider) {
        final List<?> children = provider.getChildren(parent);
        final MutableTreeNode parentNode = new DefaultMutableTreeNode(parent, children.size() > 0);
        int indexToInsert = 0;
        for (Object child : children) {
            parentNode.insert(buildTree(child, provider), indexToInsert++);
        }
        return parentNode;
    }

    /**
     * A static node comparator that groups nodes by whether they allow children or not.
     * Can set set in {@link #setNodeComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_ALLOWS_CHILDREN = (o1, o2) -> {
        final boolean allowsChildren = o1.getAllowsChildren();
        return allowsChildren == o2.getAllowsChildren() ? 0 : allowsChildren ? -1 : 1;
    };


    /******************************************************************************************************************
     *                                         Constants
     */

    protected static final String TREE_TABLE_EXPAND_NODE = "treeTableExpandNode";
    protected static final String TREE_TABLE_COLLAPSE_NODE = "treeTableCollapseNode";
    protected static final String TREE_TABLE_TOGGLE_EXPAND_NODE = "treeTableToggleExpandNode";
    protected static final int DEFAULT_COLUMN_WIDTH = 75;
    protected static final int TREE_COLUMN_INDEX = 0; // the index of the column which renders the tree - fixed at 0.

    /*
     * Immutable on construction
     */
    protected final int numColumns; //TODO: should number of columns be configurable?  Adjustments to TableColumnModel?
    protected final TreeNode rootNode;


    /******************************************************************************************************************
     *                                         Variables
     */

    /*
     * User modifiable properties of the model:
     */
    protected JTable table; // the table currently bound to the tree model, or null if not currently bound.
    protected boolean showRoot; // whether the root of the tree is shown.
    protected KeyStroke[] expandKeys = new KeyStroke[] { // key presses that expand a node.
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, false),        // + on keypad
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0, false),       // + on primary row
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK, false)}; // + above = on UK/US keyboards.
    protected KeyStroke[] collapseKeys = new KeyStroke[] { // key presses that collapse a node.
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, false),      // - on primary row
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, false)};  // - on keypad
    protected KeyStroke[] toggleKeys = new KeyStroke[] { // key presses that toggle node expansion
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)};
    protected Comparator<TreeNode> nodeComparator; // used to group nodes together based on node properties.

    /*
     * Cached/calculated properties of the model:
     */
    protected TableColumnModel columnModel;
    protected BlockModifyArrayList<TreeNode> displayedNodes = new BlockModifyArrayList<>();
    protected Map<TreeNode, Integer> expandedNodeCounts = new HashMap<>();
    protected int expandNodeCheck; // a counter which we use to perform occasional housekeeping on the set of expanded nodes.

    /*
     * Keyboard, mouse and tree events
     */
    protected MouseListener tableMouseListener;
    protected List<ExpandCollapseListener> eventListeners = new ArrayList<>(2); // tree event listeners.
    protected TreeClickHandler clickHandler; // the handler which processes expand/collapse click events.


    /******************************************************************************************************************
     *                                         Constructors
     */

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required.
     * The root node will be displayed.
     *
     * @param rootNode The root node of the tree to display.
     * @param numColumns The number of columns
     */
    public TreeTableModel(final TreeNode rootNode, final int numColumns) {
        this(rootNode, numColumns, true);
    }

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required and whether to show the root
     * of the tree.
     *
     * @param rootNode The root node of the tree to display.
     * @param numColumns The number of columns
     * @param showRoot Whether to show the root node of the tree.
     */
    public TreeTableModel(final TreeNode rootNode, final int numColumns, final boolean showRoot) {
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        this.numColumns = numColumns;
        refreshTree(true);
    }


    /******************************************************************************************************************
     *                             Abstract methods for subclasses to implement
     *
     * These methods define how a particular model obtains data, sets data and defines the columns of the table.
     * These are obviously user specific, and must be defined by the subclass.
     */

    /**
     * Returns the value of a node for a given column.
     * Subclasses should implement this to map a column to a node user object values.
     *
     * @param node The node to get a column value for.
     * @param column The column to get the value for.
     * @return The value of the column for the node.
     */
    public abstract Object getColumnValue(TreeNode node, int column);

    /**
     * Returns a TableColumn defining the column.
     *
     * @param column The column index to get the TableColumn for.
     * @return a TableColumn defining the column.
     */
    public abstract TableColumn getTableColumn(int column);


     /******************************************************************************************************************
     *                        Methods to bind and unbind this model to and from a JTable
     *
     * To use a TreeTableModel, it must be bound to a JTable.
     */

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     */
    public final void bindTable(final JTable table) {
        bindTable(table, new TreeTableRowSorter(this), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKey The default sort keys the table will have if no other sort defined.
     */
    public final void bindTable(final JTable table, final RowSorter.SortKey defaultSortKey) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKey), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     */
    public final void bindTable(final JTable table, final List<RowSorter.SortKey> defaultSortKeys) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKeys), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param rowSorter The row sorter to use with the table.
     */
    public final void bindTable(final JTable table, final RowSorter<TreeTableModel> rowSorter) {
        bindTable(table, rowSorter, new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public final void bindTable(final JTable table,  final TableCellRenderer headerRenderer) {
        bindTable(table, new TreeTableRowSorter(this), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public final void bindTable(final JTable table,  final List<RowSorter.SortKey> defaultSortKeys, final TableCellRenderer headerRenderer) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKeys), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKey The default sort key the table will have if no other sort defined.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public final void bindTable(final JTable table,  final RowSorter.SortKey defaultSortKey, final TableCellRenderer headerRenderer) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKey), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.  Cannot be null.
     * @param rowSorter The row sorter to use with the table.  Can be null if no sorting required.
     * @param headerRenderer The renderer to use to draw the table header.  Can be null if no special header rendering required.
     */
    public final void bindTable(final JTable table,
                          final RowSorter<TreeTableModel> rowSorter,
                          final TableCellRenderer headerRenderer) {
        if (this.table != null) {
            unbindTable();
        }
        this.table = table;
        if (table != null) {
            table.setAutoCreateColumnsFromModel(false);
            table.setModel(this);
            table.setColumnModel(getTableColumnModel());
            if (rowSorter != null) {
                table.setRowSorter(rowSorter);
            }
            if (headerRenderer != null) {
                table.getTableHeader().setDefaultRenderer(headerRenderer);
            }
            addKeyboardActions();
            addMouseListener();
        }
    }

    /**
     * Unbinds the model from the JTable.  Does nothing if the Jtable isn't using this as its TableModel.     *
     */
    public final void unbindTable() {
        if (table != null && table.getModel() == this) {
            removeMouseListener();
            removeKeyboardActions();
            table.setRowSorter(null);
            table.setAutoCreateColumnsFromModel(true);
            table.setModel(new DefaultTableModel());
            table = null;
        }
    }

    /******************************************************************************************************************
     *                             Optional methods for subclasses to implement
     *
     * These methods don't have to be subclassed, but you should override them if you need the features they provide.
     * They currently have blank implementations.
     */

    /**
     * Returns an icon for a given node, to be rendered against the node.
     *
     * @param node The node to get an icon for.
     * @return The icon for that node, or null if no icon is defined.
     */
    public Icon getNodeIcon(TreeNode node) {
        return null; // Default is no icons - override this method to return the icon for a node.
    }

    /**
     * Sets the value of the column to the node.  Override this method if you want to be able to edit table cells.
     * You must also specify what the table cell editors are for each TableColumn returned in getTableColumn(),
     * or implement getColumnClass() to tell JTable what default cell editors to use for basic object types.
     *
     * @param node The node to set a column value for.
     * @param column The column to set the value for.
     * @param value The value to set.
     */
    public void setColumnValue(TreeNode node, int column, Object value) {
        // Default is read-only.  Subclasses must override to set column values.
    }

    /**
     *  Override this method if you want to make table cells editable.
     *
     *  @param  rowIndex  the row
     *  @param  columnIndex the column
     *  @return false
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // Default is read-only.  Subclasses must override to set whether a cell is editable.
    }

    /**
     * Returns the class of objects for each column.  This allows the JTable to automatically pick appropriate
     * cell renderers for the types: Object, String, Double, Float, Number, Icon, IconImage and Date, and default
     * cell editors for Object, String, Boolean, and Number.
     * You can also return custom cell editors or renderers for a column by specifying them in the TableColumns you
     * return in {@link #getTableColumn(int)}
     *
     * @param column The column to return the object class for.
     * @return The class of object for values in the column.
     */
    @Override
    public Class<?> getColumnClass(final int column) {
        return Object.class; // Defaults to object, if renderers and editors not specified, JTable will not display or edit correctly.
    }

    /**
     * Returns a Comparator for a given column index.  Override this method if you want to specify custom comparators.
     * If null, then the model will compare node values directly if they implement Comparable,
     * or compare on the string value of the objects if they are not.
     *
     * @param column The column to return a Comparator for, or null if the default comparison is OK.
     * @return A Comparator for the given column, or null if no special comparator is required.
     */
    public Comparator<?> getColumnComparator(int column) {
        return null; // Defaults to no special column comparators.
    }


    /******************************************************************************************************************
     *                                         Node grouping
     *
     * Getter and setter for a node comparator.  If set, nodes will be grouped by that comparator, even if no
     * other sort columns are defined.  This allows different categories of node (e.g. files or folders) to be
     * grouped in the tree, with sorting of columns within them.
     */

    /**
     * @return a Comparator for a node, or null if not set.
     */
    public final Comparator<TreeNode> getNodeComparator() {
        return nodeComparator;
    }

    /**
     * Sets the node comparator to use, or null if no node comparisons are required.
     * When a node comparator is set, nodes are always sorted first by that comparator, before any column sorts
     * are applied.  This allows nodes to be grouped by some feature of the node.
     *
     * @param nodeComparator the node comparator to use, or null if no node comparisons are required.
     */
    public final void setNodeComparator(final Comparator<TreeNode> nodeComparator) {
        this.nodeComparator = nodeComparator;
    }


    /******************************************************************************************************************
     *                                    TableModel interface methods.
     *
     * Methods required for a JTable to interact with this class.
     */

    @Override
    public final int getRowCount() {
        return displayedNodes.size();
    }

    @Override
    public final int getColumnCount() {
        return numColumns;
    }

    @Override
    public final Object getValueAt(final int row, final int column) {
        return getColumnValue(getNodeAtRow(row), column);
    }

    @Override
    public final void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        setColumnValue(getNodeAtRow(rowIndex), columnIndex, aValue);
    }


    /******************************************************************************************************************
     *                                    Tree Change methods
     *
     * Methods required for for this model to listen for TreeModelListener events, or to manually notify the
     * model that a node has changed.
     * This class can be plugged into a DefaultTreeModel in order to respond to tree events.
     */

    @Override
    public final void treeNodesChanged(final TreeModelEvent e) {
        TreeNode parentNode = (TreeNode) e.getTreePath().getLastPathComponent();
        if (isVisible(parentNode)) {
            fireTableDataChanged(); // forces table to redraw, structure hasn't changed.
        }
    }

    public final void treeNodeInserted(final TreeNode parentNode, final int insertionIndex) {
        final int numChildren = parentNode.getChildCount();
        if (numChildren > 0 && insertionIndex >= 0 && insertionIndex < numChildren && isVisible(parentNode)) {
            final int numInserted = insertVisibleNode(parentNode, insertionIndex);
            updateVisibleChildCounts(parentNode, numInserted);
        }
    }

    @Override
    public final void treeNodesInserted(final TreeModelEvent e) {
        final TreeNode parentNode = (TreeNode) e.getTreePath().getLastPathComponent();
        final int[] childIndices = e.getChildIndices();
        treeNodesInserted(parentNode, childIndices);
    }

    public final void treeNodesInserted(final TreeNode parentNode, final int[] childIndices) {
        final int numChildren = parentNode.getChildCount();
        if (numChildren > 0 && isVisible(parentNode)) {
            final int numInserted;
            if (childIndices.length == 1) {
                final int index = childIndices[0];
                numInserted = index >= 0 && index < numChildren ? insertVisibleNode(parentNode, index) : 0;
            } else {
                numInserted = insertVisibleNodes(parentNode, childIndices);
            }
            updateVisibleChildCounts(parentNode, numInserted);
        }
    }

    protected final int insertVisibleNode(final TreeNode parentNode, final int childIndex) {
        final int insertPosition = getModelIndexAtInsertPosition(parentNode, childIndex, 1);
        if (insertPosition >= 0) {
            final TreeNode insertedNode = parentNode.getChildAt(childIndex); // The node that was inserted.
            final List<TreeNode> newNodes = new ArrayList<>();
            newNodes.add(insertedNode);
            buildVisibleChildren(insertedNode, newNodes);
            displayedNodes.addAll(insertPosition, newNodes);
            fireTableRowsInserted(insertPosition, insertPosition + newNodes.size() - 1);
            return newNodes.size();
        }
        return 0;
    }

    protected final int insertVisibleNodes(final TreeNode parentNode, final int[] childIndices) {
        final int length = childIndices.length;
        int from = 0;
        int numInserted = 0;
        while (from < length) {
            final int to = findLastConsecutiveIndex(from, childIndices); // process all consecutive insertions together.
            if (from == to) { // just inserting a single node (that potentially also has children, so numinserted may be bigger than one).
                numInserted += insertVisibleNode(parentNode, childIndices[from]);
            } else {          // inserting a block of nodes.
                numInserted += insertVisibleNodes(parentNode, childIndices, from, to);
            }
            from = to + 1;
        }
        return numInserted;
    }

    protected final int insertVisibleNodes(final TreeNode parentNode, final int[] childIndices, final int from, final int to) {
        final int insertPosition = getModelIndexAtInsertPosition(parentNode, childIndices[from], to - from + 1);
        if (insertPosition > 0) {
            final List<TreeNode> newNodes = new ArrayList<>();
            for (int index = from; index <= to; index++) {
                final int childIndex = childIndices[index];
                final TreeNode insertedNode = parentNode.getChildAt(childIndex); // The node that was inserted.
                newNodes.add(insertedNode);
                buildVisibleChildren(insertedNode, newNodes);
            }
            displayedNodes.addAll(insertPosition, newNodes);
            fireTableRowsInserted(insertPosition, insertPosition + newNodes.size() - 1);
            return newNodes.size();
        }
        return 0;
    }

    protected final int getModelIndexAtInsertPosition(final TreeNode parentNode, final int firstInsertedIndex, final int numInsertions) {
        final int lastInsertedIndex = firstInsertedIndex + numInsertions - 1;
        final int numChildren = parentNode.getChildCount();
        final int modelIndex;
        if (numChildren == numInsertions) {                        // No previous nodes (inserted into parent with no prior children):
            modelIndex = getModelIndex(parentNode) + 1;                                             //  - insert at the position just after the parent node.
        } else if (lastInsertedIndex == numChildren - 1) {         // If inserted at the end of existing children:
            final TreeNode previousChild = parentNode.getChildAt(firstInsertedIndex - 1);           //  previous last child is the one before the first inserted..
            modelIndex = getModelIndex(previousChild) + expandedNodeCounts.get(previousChild) + 1;  // - insert one after last previous child and all its visible children.
        } else {                                                   // Inserting before the end of existing children:
            modelIndex = getModelIndex(parentNode.getChildAt(lastInsertedIndex + 1));               // - insert at the index of the child the last inserted node displaced one on.
        }
        return modelIndex;
    }

    protected final int findLastConsecutiveIndex(final int from, final int[] indices) {
        int lastConsecutiveIndex = from;
        for (int i = from + 1; i < indices.length; i++) {
            if (indices[i] - indices[i - 1] == 1) {
                lastConsecutiveIndex = i; // one apart - update last consecutive index.
            } else {
                break; // not consecutive - cease processing.
            }
        }
        return lastConsecutiveIndex;
    }

    /*
    private int getModelIndexAtInsertPosition(final TreeNode parentNode, final int[] childIndices, final int from, final int to) {
        final int numInsertions = to - from + 1;
        final int lastInsertedIndex = childIndices[to];
        final int firstInsertedIndex = lastInsertedIndex - numInsertions + 1;
        final int numChildren = parentNode.getChildCount();
        final int modelIndex;
        if (numChildren == numInsertions) {                        // No previous nodes (inserted into parent with no prior children):
            modelIndex = getModelIndex(parentNode) + 1;                                             //  - insert at the position just after the parent node.
        } else if (lastInsertedIndex == numChildren - 1) {         // If inserted at the end of existing children:
            final TreeNode previousChild = parentNode.getChildAt(firstInsertedIndex - 1);           //  previous last child is the one before the first inserted..
            modelIndex = getModelIndex(previousChild) + expandedNodeCounts.get(previousChild) + 1;  // - insert one after last previous child and all its visible children.
        } else {                                                   // Inserting before the end of existing children:
            modelIndex = getModelIndex(parentNode.getChildAt(lastInsertedIndex + 1));               // - insert at the index of the child the last inserted node displaced one on.
        }
        return modelIndex;
    }

    protected final int getModelIndexAtInsertPosition(final TreeNode parentNode, final int childIndex) {
        final int lastChildIndex = parentNode.getChildCount() - 1;
        final int modelIndex;
        if (lastChildIndex == 0) {                         // No previous node (inserted into parent with no children):
            modelIndex = getModelIndex(parentNode) + 1;                                             //  - insert at the position just after the parent node.
        } else if (childIndex == lastChildIndex) {         // If inserting at the end of existing children:
            final TreeNode previousChild = parentNode.getChildAt(childIndex - 1);                   //  previous last child is the one before it now.
            modelIndex = getModelIndex(previousChild) + expandedNodeCounts.get(previousChild) + 1;  // - insert one after last previous child and all its visible children.
        } else {                                           // Inserting before the end of existing children:
            modelIndex = getModelIndex(parentNode.getChildAt(childIndex + 1));                      // - insert at the index of the child this node displaced one on.
        }
        return modelIndex;
    }

     */



    @Override
    public final void treeNodesRemoved(final TreeModelEvent e) {
        TreeNode parentNode = (TreeNode) e.getTreePath().getLastPathComponent();
        if (isVisible(parentNode)) {
            final int previouslyVisibleChildren = expandedNodeCounts.get(parentNode);
            final int modelIndex = getModelIndex(parentNode);
            displayedNodes.replace(modelIndex + 1, modelIndex + previouslyVisibleChildren,
                    parentNode.children(), parentNode.getChildCount());

            //TODO: fire table change events.
        }
    }

    @Override
    public final void treeStructureChanged(final TreeModelEvent e) {
        TreeNode parentNode = (TreeNode) e.getTreePath().getLastPathComponent();
        if (isVisible(parentNode)) {
            //TODO: structure change handling.
            //      refresh entire tree, or a subset?
        }
    }

    /******************************************************************************************************************
     *                                             Refreshment
     *
     * When you change the tree nodes or children, you should refresh the model,
     * or the changes won't be displayed immediately, if ever.
     */

    //TODO: do we need refresh methods with change methods above?

    /**
     * Refreshes the display of a node.  Call this when a node's displayable values have changed.
     *
     * @param node The node whose displayable values have changed.
     */
    public void refreshNode(final TreeNode node) {
        if (isVisible(node)) {
            //TODO: refresh this part of the table.
        }
    }

    /**
     * Notifies the TreeTableModel that the children of a node have changed.
     * This must be called whenever children of nodes are modified outside of an expand or collapse event.
     * It is not necessary to call this if you are a listener of TreeTableEvents
     * and are dynamically adding or removing child nodes inside those events; these changes are already handled.
     * @param parentNode The node whose children have changed.
     */
    public void refreshChildren(final TreeNode parentNode) {
        if (isVisible(parentNode)) {
            //TODO: refresh this part of the tree.
        }
    }

    /**
     * Refreshes the entire visible tree, rebuilding from the root upwards.
     * If you require a rebuild from scratch, then reset expanded nodes as well, as none of the existing expanded
     * nodes will be in the new tree. If you simply want to rebuild due to some node changes,
     * but would like to preserve expanded state of existing nodes if possible, then pass false to resetExpanded.
     *
     * @param resetExpanded Whether to reset expanded nodes.
     */
    public void refreshTree(final boolean resetExpanded) {
        if (resetExpanded) {
            resetExpanded();   // wipe out any existing expansion, add the root as expanded if it's not visible.
        } else {
            removeDeadNodes(); // remove any expanded nodes which aren't part of the tree anymore.
        }
        buildVisibleNodes();
    }

    /******************************************************************************************************************
     *                                          Node getters
     *
     * Methods to get nodes, converting between model and table indexes.
     */

    /**
     * @return the root node of the tree.
     */
    public final TreeNode getRoot() {
        return rootNode;
    }

    /**
     * Gets the node at the model row.
     *
     * @param modelIndex The row in the display model to get the node for.
     * @return The node for the row in the (unsorted) model index.
     */
    public final TreeNode getNodeAtRow(final int modelIndex) {
        return modelIndex >= 0 && modelIndex < displayedNodes.size() ? displayedNodes.get(modelIndex) : null;
    }

    /**
     * Gets the node in a bound JTable given a table row index.
     * The view row index can differ from the rows in this underlying model if the table is sorted or filtered.
     *
     * @param tableIndex The row in the table to get the node from.
     * @return The node at the tableRow position in the JTable.
     */
    public final TreeNode getNodeAtTableRow(final int tableIndex) {
        return getNodeAtRow(getModelIndex(tableIndex));
    }

    /**
     * Gets the first node which is currently selected in the JTable, or null if no row is selected.
     *
     * @return the node which is currently selected in the JTable, or null if no row is selected.
     */
    public final TreeNode getSelectedNode() {
        return table == null? null : getNodeAtTableRow(table.getSelectedRow());
    }

    /**
     * Gets a list of all the selected nodes in the JTable, or an empty list if no rows are selected.
     * @return a list of all the selected nodes in the JTable, or an empty list if no rows are selected.
     */
    public final List<TreeNode> getSelectedNodes() {
        List<TreeNode> nodes = new ArrayList<>();
        if (table != null) {
            for (int rowIndex : table.getSelectedRows()) {
                nodes.add(getNodeAtTableRow(rowIndex));
            }
        }
        return nodes;
    }

    /**
     * Gets the index in the model of a row in a bound table.
     * If the table is unsorted, the model and table indexes will be identical.
     * If the table is sorted, then the row sorter can provide a mapping from the row in the visible table
     * to the row in this underlying model.
     *
     * @param tableIndex The row in the JTable to get the model index for.
     * @return the model index of a row in a bound JTable.
     */
    public final int getModelIndex(final int tableIndex) {
        final RowSorter<? extends TableModel> rowSorter = table == null? null : table.getRowSorter();
        return rowSorter == null? tableIndex : rowSorter.convertRowIndexToModel(tableIndex);
    }

    //TODO: could also track index of each expanded node, then could ask for index of a node given the index of the parent node to start from.
    /**
     * Gets the index in the model of a node, or -1 if it isn't visible or part of the current tree.
     *
     * @param node The node to get the model index for.
     * @return The index in the model of displayed nodes, or -1 if it isn't being displayed.
     */
    public final int getModelIndex(final TreeNode node) {
        if (isVisible(node)) {                   // node visibility check just looks at parents up to root.
            return displayedNodes.indexOf(node); // indexOf is a linear search of all displayed nodes
        }
        return -1; // not visible - not in displayed nodes.
    }


    /******************************************************************************************************************
     *                                          Root node visibility
     */

    /**
     * @return true if the root node will be displayed, false if not.
     */
    public final boolean getShowRoot() {
        return showRoot;
    }

    /**
     * Sets whether the root node should be displayed or not, and updates the display model if the state changes.
     *
     * @param showRoot whether the root node should be displayed or not.
     */
    public final void setShowRoot(final boolean showRoot) {
        if (this.showRoot != showRoot) {
            this.showRoot = showRoot;
            if (showRoot) {
                displayedNodes.add(0, rootNode);
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
    public final void addExpandCollapseListener(final ExpandCollapseListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes a TreeTableEvent listener.
     *
     * @param listener The listener to remove.
     */
    public final void removeExpandCollapseListener(final ExpandCollapseListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Adds a mouse listener for expand / collapse events to a JTable
     **/
    protected final void addMouseListener() {
        if (table != null) {
            tableMouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleExpansion(e);
                }
            };
            table.addMouseListener(tableMouseListener);
        }
    }

    /**
     * Removes a mouse listener for expand / collapse events
     * from a JTable previously registered by a call to addMouseListener().
     *
     */
    protected final void removeMouseListener() {
        if (table != null) {
            table.removeMouseListener(tableMouseListener);
        }
    }

    /**
     * Adds keyboard actions for + (expand), - (collapse) and space (toggle) selected node expansion.
     */
    protected final void addKeyboardActions() {
        if (table != null) {
            final InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
            for (KeyStroke keyStroke : expandKeys) {
                inputMap.put(keyStroke, TREE_TABLE_EXPAND_NODE);
            }
            for (KeyStroke keyStroke : collapseKeys) {
                inputMap.put(keyStroke, TREE_TABLE_COLLAPSE_NODE);
            }
            for (KeyStroke keyStroke : toggleKeys) {
                inputMap.put(keyStroke, TREE_TABLE_TOGGLE_EXPAND_NODE);
            }
            final ActionMap actionMap = table.getActionMap();
            actionMap.put(TREE_TABLE_EXPAND_NODE, new TreeTableExpandAction());
            actionMap.put(TREE_TABLE_COLLAPSE_NODE, new TreeTableCollapseAction());
            actionMap.put(TREE_TABLE_TOGGLE_EXPAND_NODE, new TreeTableToggleExpandAction());
        }
    }

    /**
     * Removes keyboard actions for + (expand), - (collapse) and space (toggle) selected node expansion.
     */
    protected final void removeKeyboardActions() {
        if (table != null) {
            final InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
            for (KeyStroke keyStroke : expandKeys) {
                inputMap.remove(keyStroke);
            }
            for (KeyStroke keyStroke : collapseKeys) {
                inputMap.remove(keyStroke);
            }
            for (KeyStroke keyStroke : toggleKeys) {
                inputMap.remove(keyStroke);
            }
            final ActionMap actionMap = table.getActionMap();
            actionMap.remove(TREE_TABLE_EXPAND_NODE);
            actionMap.remove(TREE_TABLE_COLLAPSE_NODE);
            actionMap.remove(TREE_TABLE_TOGGLE_EXPAND_NODE);
        }
    }

    /**
     * Sets the click handler for the primary tree column.  If not set explicitly, it will either default
     * to the primary TableCellRenderer if it implements the TreeClickHandler interface, or a very rough default
     * handler will be used that estimates the likely node indent for a tree when processing clicks.
     *
     * @param clickHandler The TreeClickHandler to process expand/collapse events for the primary tree column.
     */
    public final void setTreeClickHandler(final TreeClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }


    /******************************************************************************************************************
     *                                          Column methods
     */

    /**
     * @return the TableColumnModel for this TreeTableModel.
     */
    public final TableColumnModel getTableColumnModel() {
        if (columnModel == null) {
            columnModel = new DefaultTableColumnModel();
            for (int column = 0; column < getColumnCount(); column++) {
                TableColumn tcolumn = getTableColumn(column);
                if (column == TREE_COLUMN_INDEX && tcolumn.getCellRenderer() == null) {
                    tcolumn.setCellRenderer(new TreeTableCellRenderer(this));
                }
                columnModel.addColumn(tcolumn);
            }
        }
        return columnModel;
    }

    /**
     * Checks that a column is a valid column index, and throws an IllegalArgumentException if it is not.
     * @param column The model column index.
     * @throws IllegalArgumentException if the column index is not valid.
     */
    protected final void checkValidColumn(final int column) {
        if (column >= numColumns || column < 0) {
            throw new IllegalArgumentException("Columns must be between 0 and " + (numColumns - 1) + ". Column value was: " + column);
        }
    }

    protected final TableColumn createColumn(final int modelIndex, final Object headerValue) {
        return createColumn(modelIndex, DEFAULT_COLUMN_WIDTH, null, null, headerValue);
    }

    protected final TableColumn createColumn(final int modelIndex, final TableCellRenderer cellRenderer, final Object headerValue) {
        return createColumn(modelIndex, DEFAULT_COLUMN_WIDTH, cellRenderer, null, headerValue);
    }

    protected final TableColumn createColumn(final int modelIndex,
                                    final TableCellRenderer cellRenderer, final TableCellEditor cellEditor,
                                    final Object headerValue) {
        return createColumn(modelIndex, DEFAULT_COLUMN_WIDTH, cellRenderer, cellEditor, headerValue);
    }

    protected final TableColumn createColumn(final int modelIndex, final int width,
                                    final TableCellRenderer cellRenderer, final TableCellEditor cellEditor,
                                    final Object headerValue) {
        final TableColumn column = new TableColumn(modelIndex, width, cellRenderer, cellEditor);
        column.setHeaderValue(headerValue);
        return column;
    }

    /******************************************************************************************************************
     *                             Key bindings for expand and collapse and toggle.
     */

     public void setExpandKeys(final KeyStroke... expandKeys) {
         this.expandKeys = expandKeys;
     }

     public void setCollapseKeys(final KeyStroke... collapseKeys) {
         this.collapseKeys = collapseKeys;
     }

     public void setToggleKeys(final KeyStroke... toggleKeys) {
         this.toggleKeys = toggleKeys;
     }

     public KeyStroke[] getExpandKeys() {
         return expandKeys;
     }

     public KeyStroke[] getCollapseKeys() {
         return collapseKeys;
     }

     public KeyStroke[] getToggleKeys() {
         return toggleKeys;
     }


    /******************************************************************************************************************
     *                                Node expansion and collapse and tree change.
     */

    /**
     * @param node The node to check.
     * @return true if the node is expanded.
     */
    public boolean isExpanded(final TreeNode node) {
        return expandedNodeCounts.containsKey(node);
    }

    /**
     * @return a read only set of tree nodes which are currently expanded.  This will change as expanded nodes change.
     */
    public Set<TreeNode> getExpandedNodes() {
        return expandedNodeCounts.keySet();
    }

    /**
     * Sets a node to expand, if it isn't already expanded.
     * @param node The node to expand.
     */
    public final void expandNode(final TreeNode node) {
        if (!isExpanded(node)) {
            toggleExpansion(node, getModelIndex(node)); // if node is not visible, will have index of -1.
        }
    }

    /**
     * Collapses a node if it is already expanded.
     * @param node The node to collapse.
     */
    public final void collapseNode(final TreeNode node) {
        if (isExpanded(node)) {
            toggleExpansion(node, getModelIndex(node)); // if node is not visible, will have index of -1.
        }
    }

    /**
     * Listeners may change the node structure (e.g. dynamically add, remove or change nodes).
     * If we're going to remove nodes (currently expanded), we need to get the number of nodes that are
     * *currently* visible, before any listeners run, so we know how many current nodes to remove, and
     * whether the node was already expanded or not.
     *
     * @param node The node to toggle expansion.
     * @param modelRow The row in the model the node exists at.
     */
    protected final void toggleExpansion(final TreeNode node, final int modelRow) {
        final boolean expanded = isExpanded(node);
        if (modelRow >= 0 && modelRow < displayedNodes.size()) { // a visible node: - have to track changes and notify table of data changes.
            if (listenersApprove(node, expanded)) {
                boolean tableNotified = false; // track whether we tell the table it's been changed yet.

                // If we had some visible nodes before toggling to unexpanded, remove those children.
                if (expanded) {
                    tableNotified |= removeDisplayedChildren(modelRow, expandedNodeCounts.get(node));
                }

                // Toggle the expanded state of the node.
                setExpanded(node, !expanded);

                // If we weren't already expanded before toggling to expanded, add the new children.
                if (!expanded) {
                    tableNotified |= addChildrenToDisplay(modelRow, node);
                }

                // If we haven't removed or added any children, notify the table this one node may have changed:
                if (!tableNotified) {
                    fireTableRowsUpdated(modelRow, modelRow);
                }
            }
        } else { // toggling a node that isn't a visible row - just get approval from listeners, then toggle the expanded state.
            if (listenersApprove(node, expanded)) {
                setExpanded(node, !expanded);
            }
        }
    }

    protected void setExpanded(final TreeNode node, final boolean expanded) {
        if (expanded) {
            expandVisibleChildCounts(node);
            checkExpandedNodes();
        } else {
            collapseVisibleChildCounts(node);
        }
    }

    protected void expandVisibleChildCounts(final TreeNode node) {
        final Map<TreeNode, Integer> localTreeCounts = expandedNodeCounts;
        final int newVisibleChildren = calculateVisibleChildNodes(node, true);
        localTreeCounts.put(node, newVisibleChildren);
        TreeNode parentNode = node.getParent();
        while (parentNode != null) {
            final int parentChildCount = localTreeCounts.get(parentNode);
            localTreeCounts.put(parentNode, parentChildCount + newVisibleChildren);
            parentNode = parentNode.getParent();
        }
    }

    protected void updateVisibleChildCounts(final TreeNode node, final int delta) {
        final Map<TreeNode, Integer> localTreeCounts = expandedNodeCounts;
        final int newVisibleChildren = localTreeCounts.get(node) + delta;
        localTreeCounts.put(node, newVisibleChildren);
        TreeNode parentNode = node.getParent();
        while (parentNode != null) {
            final int parentChildCount = localTreeCounts.get(parentNode);
            localTreeCounts.put(parentNode, parentChildCount + delta);
            parentNode = parentNode.getParent();
        }
    }

    protected void collapseVisibleChildCounts(TreeNode node) {
        final Map<TreeNode, Integer> localTreeCounts = expandedNodeCounts;
        final int oldVisibleChildren = localTreeCounts.remove(node);
        TreeNode parentNode = node.getParent();
        while (parentNode != null) {
            final int parentChildCount = localTreeCounts.get(parentNode);
            localTreeCounts.put(parentNode, parentChildCount - oldVisibleChildren);
            parentNode = parentNode.getParent();
        }
    }

    /**
     * Finds previously expanded nodes which are no longer part of the tree and removes them from the set of expanded nodes.
     * A node is no longer part of the tree if the root of its tree is not the root of our tree.
     * Nodes can be removed at any time, and this is not under control of the model.  If there were previously recorded
     * as expanded, there will be a memory leak in our expanded node tracking set, and the set of expanded nodes will grow gradually forever.
     * Does this every 50 expansions to avoid an unnecessary performance hit, but to reclaim
     * memory for nodes that were previously expanded but are no longer part of the tree.
     */
    protected final void checkExpandedNodes() {
        if (expandNodeCheck++ >= 50) {
            expandNodeCheck = 0;
            removeDeadNodes();
        }
    }

    protected final void resetExpanded() {
        expandedNodeCounts.clear();
        if (!showRoot) { // expand the root if it's not showing.
            expandNode(rootNode);
        }
    }

    protected final void removeDeadNodes() {
        final Iterator<Map.Entry<TreeNode, Integer>> iterator = expandedNodeCounts.entrySet().iterator();
        while (iterator.hasNext()) {
            final TreeNode node = iterator.next().getKey();
            if (!nodeInTree(node)) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns true if this node is rooted in the model tree.
     *
     * @param node The node to check.
     * @return true if the node is rooted in the current tree.
     */
    protected final boolean nodeInTree(final TreeNode node) {
        return getRoot(node) == rootNode;
    }

    /**
     * Gets the root node for a node (root node has a null parent).
     *
     * @param node The node to get the root for.
     * @return The root node for this node (can be the same node).
     */
    protected final TreeNode getRoot(final TreeNode node) {
        TreeNode currentNode = node;
        while (currentNode.getParent() != null) {
            currentNode = currentNode.getParent();
        }
        return currentNode;
    }

    /**
     * Toggles expansion of nodes based on a mouse click.
     *
     * @param evt The mouse event to process.
     */
    protected final void toggleExpansion(final MouseEvent evt) {
        if (table != null) {
            final Point point = evt.getPoint();
            final int tableRow = table.rowAtPoint(point);
            final int columnIndex = table.columnAtPoint(point);
            final int modelRow = getModelIndex(tableRow);
            final TreeNode node = displayedNodes.get(modelRow);
            if (getClickHandler().clickOnExpand(node, columnIndex, evt)) {
                toggleExpansion(node, modelRow);
            }
        }
    }

    /******************************************************************************************************************
     *                                         Visible node management.
     */

    protected final void buildVisibleNodes() {
        displayedNodes.clear();
        if (showRoot) {
            displayedNodes.add(rootNode);
        }
        buildVisibleChildren(rootNode, displayedNodes);
    }

    protected final boolean removeDisplayedChildren(final int modelRow, final int numberToRemove) {
        if (numberToRemove > 0) {
            final int firstChildRow = modelRow + 1;
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

    protected final boolean addChildrenToDisplay(final int modelRow, final TreeNode node) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            final int firstChildRow = modelRow + 1;
            final List<TreeNode> newVisibleNodes = buildVisibleChildren(node);
            if (newVisibleNodes.size() == 1) {
                displayedNodes.add(firstChildRow, newVisibleNodes.get(0));
            } else {
                displayedNodes.addAll(firstChildRow, newVisibleNodes);
            }
            final int lastChildRow = firstChildRow + newVisibleNodes.size() - 1;
            fireTableRowsInserted(firstChildRow, lastChildRow);
            return true;
        }
        return false;
    }

    protected final List<TreeNode> buildVisibleChildren(final TreeNode node) {
        if (node.getChildCount() > 0) {
            final List<TreeNode> children = new ArrayList<>();
            buildVisibleChildren(node, children);
            return children;
        }
        return Collections.emptyList();
    }

    protected final void buildVisibleChildren(final TreeNode node, final List<TreeNode> visibleNodes) {
        if (isExpanded(node)) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
                final TreeNode child = node.getChildAt(childIndex);
                visibleNodes.add(child);
                buildVisibleChildren(child, visibleNodes); // And the children of the child if they're visible...
            }
        }
    }

    /**
     * Calculates how many child nodes will be visible given a parent node.
     * If you want a count of the visible nodes of a parent before it is actually expanded,
     * you can say to expand the parent (no matter what it's actual expanded status right now).
     *
     * @param node The node to get a count of visible children for.
     * @param expandParent true if you want a count no matter whether it is currently expanded or not.
     * @return A count of all children which would be visible in this parent.
     */
    protected final int calculateVisibleChildNodes(final TreeNode node, boolean expandParent) {
        int totalVisibleChildNodes = 0;
        if (expandParent || isExpanded(node)) {
            for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
                final TreeNode child = node.getChildAt(childIndex);
                totalVisibleChildNodes += (1 + calculateVisibleChildNodes(child, false));
            }
        }
        return totalVisibleChildNodes;
    }

    protected final boolean isVisible(final TreeNode node) {
        return node == rootNode? showRoot : parentsAreExpandedAndPartOfTree(node);
    }

    protected final boolean parentsAreExpandedAndPartOfTree(final TreeNode node) {
        TreeNode parentNode, currentNode = node;
        while ((parentNode = currentNode.getParent()) != null) {
            if (!isExpanded(parentNode)) {
                return false;
            }
            currentNode = parentNode;
        }
        // All parents are expanded if we get to a root, AND the root IS the tree root node.
        // If we have a node with a null parent which isn't the root,
        // then it's not part of this tree and thus cannot be visible in it.
        return currentNode == rootNode;
    }


    /******************************************************************************************************************
     *                                            Listener management
     */

    protected final boolean listenersApprove(final TreeNode node, final boolean isCollapsing) {
        return isCollapsing ? listenersApprovedCollapseEvent(node) : listenersApprovedExpandEvent(node);
    }

    protected final boolean listenersApprovedExpandEvent(final TreeNode node) {
        for (ExpandCollapseListener listener : eventListeners) {
            if (!listener.nodeExpanding(node)) {
                return false;
            }
        }
        return true;
    }

    protected final boolean listenersApprovedCollapseEvent(final TreeNode node) {
        for (ExpandCollapseListener listener : eventListeners) {
            if (!listener.nodeCollapsing(node)) {
                return false;
            }
        }
        return true;
    }

    protected final TreeClickHandler getClickHandler() {
        if (clickHandler == null) {
            final TableCellRenderer renderer = getColumnWithModelIndex(TREE_COLUMN_INDEX).getCellRenderer();
            if (renderer instanceof TreeClickHandler) {
                clickHandler = (TreeClickHandler) renderer;
            } else {
                clickHandler = new TreeTableCellRenderer(this); // implements click handler.
            }
        }
        return clickHandler;
    }

    protected final TableColumn getColumnWithModelIndex(final int modelIndex) {
        TableColumnModel model = getTableColumnModel();
        for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
            final TableColumn column = model.getColumn(columnIndex);
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
       throw new IllegalArgumentException("Invalid model index: " + modelIndex);
    }

    /******************************************************************************************************************
     *                            Action classes to bind to keyboard events.
     */

    protected class TreeTableExpandAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JTable) {
                final JTable table = (JTable) source;
                final int modelIndexRow = getModelIndex(table.getSelectedRow());
                final TreeNode node = getNodeAtRow(modelIndexRow);
                if (node != null && node.getAllowsChildren() && !isExpanded(node)) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }
    }

    protected class TreeTableCollapseAction  extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JTable) {
                final JTable table = (JTable) source;
                final int modelIndexRow = getModelIndex(table.getSelectedRow());
                final TreeNode node = getNodeAtRow(modelIndexRow);
                if (node != null && node.getAllowsChildren() && isExpanded(node)) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }
    }

    protected class TreeTableToggleExpandAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JTable) {
                final JTable table = (JTable) source;
                final int modelIndexRow = getModelIndex(table.getSelectedRow());
                final TreeNode node = getNodeAtRow(modelIndexRow);
                if (node != null) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }
    }

    /******************************************************************************************************************
     *                                              Interfaces
     */

    /**
     * An interface which decides if a click on a node is an expand or collapse click.
     */
    public interface TreeClickHandler {

        /**
         * Decides if a click on a node is an expand or collapse click. returns true if it is.
         * <p>
         * Note that the column is the index of the column clicked in the TableColumnModel, and that mouse coordinates
         * are given in terms of the whole JTable.  So if your tree column has been moved to the right, for example, you
         * need to calculate the start position of that column.  There is a method on the TreeTableModel which can
         * calculate how much space columns to the left of a column take up (calculateWidthToLeft).
         *
         * @param node The node which was clicked on.
         * @param column The index of the column in the TableColumnModel which was clicked.
         *               Note that if the columns were dragged into new positions, the column model index will not match
         *               the index in the TableColumnModel.  If you need the column model index, get it from the TableColumn.
         * @param evt  The mouse event
         * @return true if there was a click on expand/collapse for this node.
         */
        boolean clickOnExpand(TreeNode node, int column, MouseEvent evt);
    }

    /**
     * An interface for a listener to expand or collapse events in the tree.
     */
    public interface ExpandCollapseListener {

        /**
         * Notifies the listener that the tree node will expand.
         * @param node the node that is about to expand
         * @return true if you want to allow the expansion.
         */
        boolean nodeExpanding(TreeNode node);

        /**
         * Notifies the listener that the tree node will collapse.
         * @param node the node that is about to collapse
         * @return true if you want to allow the collapse.
         */
        boolean nodeCollapsing(TreeNode node);
    }

    /**
     * An interface to an object that can return a list of objects as its children.
     * Used to build trees of TreeTableNodes from a root object, and this method which provides the object children.
     */
    public interface ChildProvider {

        /**
         * Returns a list of child objects given the parent.
         * @param parent The object to get the children of.
         * @return the children of the parent object, or empty if no children.  Should not return null.
         */
        List<?> getChildren(Object parent);
    }

}
