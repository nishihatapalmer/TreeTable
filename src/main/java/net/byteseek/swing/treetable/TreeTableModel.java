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

//TODO: review protected status of some methods - should they be public?
//      Just decided isVisible() was a good candidate.  Info methods should be
//      reasonably safe if they're fundamental operations.

//TODO: explain using tree models to update the tree, or refresh() calls if you don't want to do it that way.

//TODO: profile large trees, sorting, inserting/removing nodes dynamically, etc.  Pay attention to getModelIndex() - linear scan on insertion/deletion.

//TODO: expand-all by default option?  All nodes always visible?

//TODO: dead node removal interferes with node expansion of nodes not in tree...?

//TODO: insert-expanded option?

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

    /* *****************************************************************************************************************
     *                                         Static Utility Methods
     */

    /**
     * Mirrors a tree of user objects as a tree of MutableTreeNodes, with each MutableTreeNode associated with the
     * appropriate user object.  If your user objects already have a tree structure, this is a quick way to
     * obtain a parallel tree of TreeNodes.
     *
     * <p>
     * It will build a root MutableTreeNode and all sub children given the user object which is the parent,
     * and a ChildProvider method which returns the list of children for user objects in the tree.
     * This can be provided as a one line lambda expression, or by implementing the TreeTableModel.ChildProvider interface.
     * <p>
     * Note - this will not keep the tree of MutableTreeNodes up to date if your user object tree structure changes.
     * You must inform the TreeTableModel of any changes to your tree structure to keep it in sync.
     *
     * @param parent The user object which is at the root of the tree.
     * @param provider An object which provides a list of user objects from the parent user object.
     * @return A DefaultMutableTreeNode with all child nodes built and associated with their corresponding user objects.
     */
    public static MutableTreeNode buildTree(final Object parent, final TreeTableModel.ChildProvider provider) {
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
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link #setNodeComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_ALLOWS_CHILDREN = (o1, o2) -> {
        final boolean allowsChildren = o1.getAllowsChildren();
        return allowsChildren == o2.getAllowsChildren() ? 0 : allowsChildren ? -1 : 1;
    };


    /* *****************************************************************************************************************
     *                                         Constants
     */

    /*
     * Keys to register keystroke events against.
     */
    protected static final String EXPAND_NODE_KEYSTROKE_KEY = "treeTableExpandNode";
    protected static final String COLLAPSE_NODE_KEYSTROKE_KEY = "treeTableCollapseNode";
    protected static final String TOGGLE_EXPAND_NODE_KEYSTROKE_KEY = "treeTableToggleExpandNode";

    /**
     * The TableColumnModel model index of the column which renders the tree structure and responds to clicks on expand
     * or collapse handles.  This is fixed at 0, as making it mutable caused various issues with re-assigning renderers
     * and handlers. It was doable, but overly complex.  It is simpler to say that the first column in the TableColumnModel
     * is the column that handles the tree events. You can move columns in the TableColumnModel, so it does not display as the
     * first column, but it is always logically the first column.
     */
    protected static final int TREE_COLUMN_INDEX = 0;

    /**
     * Default visible width of a TableColumn created using the utility createColumn() methods, if you don't specify a width.
     */
    protected static final int DEFAULT_COLUMN_WIDTH = 75;


    /* *****************************************************************************************************************
     *                                         Variables
     */

    /*
     * User modifiable properties of the model:
     */

    /**
     * The root node of the tree with which the model is initialised.  The root can be changed later  by calling setRoot().
     */
    protected TreeNode rootNode;

    /**
     * The JTable this TreeTableModel is current bound to.  Use bindTable() methods or unbindTable() methods to control.
     */
    protected JTable table; // the table currently bound to the tree model, or null if not currently bound.

    /**
     * Whether the root of the tree is visible in the tree or not.  Can be changed with setShowRoot().
     */
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
    protected final BlockModifyArrayList<TreeNode> displayedNodes = new BlockModifyArrayList<>(); // has block operations which are more efficient than inserting or removing individually.
    protected final Map<TreeNode, Integer> expandedNodeCounts = new WeakHashMap<>(); // If a TreeNode is removed and no longer used, it will be garbage collected.
    protected TableCellRenderer oldHeaderRenderer; // Holds on to the previous header renderer for a JTable.  When unbinding, the original renderer is replaced.

    /*
     * Keyboard, mouse and tree events
     */
    protected MouseListener tableMouseListener;
    protected final List<ExpandCollapseListener> eventListeners = new ArrayList<>(2); // tree event listeners.
    protected TreeClickHandler clickHandler; // the handler which processes expand/collapse click events.


    /* *****************************************************************************************************************
     *                                         Constructors
     */

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required.
     * The root node will be displayed.
     *
     * @param rootNode The root node of the tree to display.
     * @throws IllegalArgumentException if the rootNode is null.
     */
    public TreeTableModel(final TreeNode rootNode) {
        this(rootNode, true);
    }

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required and whether to show the root
     * of the tree.
     *
     * @param rootNode The root node of the tree to display.
     * @param showRoot Whether to show the root node of the tree.
     * @throws IllegalArgumentException if the rootNode is null.
     */
    public TreeTableModel(final TreeNode rootNode, final boolean showRoot) {
        checkNull(rootNode, "rootNode");
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        refreshTree(true);
    }


    /* *****************************************************************************************************************
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
     * Creates a TableColumnModel to reflect what columns the JTable should display for your subclass.
     * This could be all available columns, or you can just add a few and add or remove them dynamically using
     * the model later.  The model used is cached and can be obtained by a call to getTableColumnModel().
     * <p>
     * If no special TreeCellRenderer is set for the tree column with model index zero, then a default tree
     * renderer will be added automatically after it is created.
     * <p>
     * You should generally include a TableColumn with model index zero, as that is the column that renders the
     * tree structure, and responds to clicks on expand or collapse handles.
     *
     * @return A TableColumnModel with the columns you wish to display in the tree table initially.
     */
    public abstract TableColumnModel createTableColumnModel();

     /* *****************************************************************************************************************
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
    public void bindTable(final JTable table) {
        bindTable(table, new TreeTableRowSorter(this), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKey The default sort keys the table will have if no other sort defined.
     */
    public void bindTable(final JTable table, final RowSorter.SortKey defaultSortKey) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKey), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     */
    public void bindTable(final JTable table, final List<RowSorter.SortKey> defaultSortKeys) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKeys), new TreeTableHeaderRenderer());
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
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public void bindTable(final JTable table,  final List<RowSorter.SortKey> defaultSortKeys, final TableCellRenderer headerRenderer) {
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
    public void bindTable(final JTable table,  final RowSorter.SortKey defaultSortKey, final TableCellRenderer headerRenderer) {
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
    public void bindTable(final JTable table,
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
            oldHeaderRenderer = table.getTableHeader().getDefaultRenderer();
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
    public void unbindTable() {
        if (table != null && table.getModel() == this) {
            removeMouseListener();
            removeKeyboardActions();
            table.setColumnModel(new DefaultTableColumnModel());
            table.setRowSorter(null);
            table.setAutoCreateColumnsFromModel(true); //TODO: should we cache old table setting?
            table.setModel(new DefaultTableModel());
            table.getTableHeader().setDefaultRenderer(oldHeaderRenderer);
            table = null;
            oldHeaderRenderer = null;
        }
    }

    /**
     * @return the JTable bound to this TreeTableModel, or null if one is not currently bound.
     */
    public JTable getTable() {
        return table;
    }

    /* *****************************************************************************************************************
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
     * cell editors for Object, String, Boolean, and Number, if you have not specified your own in the TableColumns.
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


    /* *****************************************************************************************************************
     *                                         Node grouping
     *
     * Getter and setter for a node comparator.  If set, nodes will be grouped by that comparator, even if no
     * other sort columns are defined.  This allows different categories of node (e.g. files or folders) to be
     * grouped in the tree, with sorting of columns within them.
     */

    /**
     * @return a Comparator for a node, or null if not set.
     */
    public Comparator<TreeNode> getNodeComparator() {
        return nodeComparator;
    }

    /**
     * Sets the node comparator to use, or null if no node comparisons are required.
     * When a node comparator is set, nodes are always sorted first by that comparator, before any column sorts
     * are applied.  This allows nodes to be grouped by some feature of the node.
     *
     * @param nodeComparator the node comparator to use, or null if no node comparisons are required.
     */
    public void setNodeComparator(final Comparator<TreeNode> nodeComparator) {
        this.nodeComparator = nodeComparator;
    }


    /* *****************************************************************************************************************
     *                                    TableModel interface methods.
     *
     * Methods required for a JTable to interact with this class.
     */

    @Override
    public int getRowCount() {
        return displayedNodes.size();
    }

    @Override
    public int getColumnCount() {
        return getTableColumnModel().getColumnCount();
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return getColumnValue( getNodeAtRow(row), column);
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        setColumnValue( getNodeAtRow(rowIndex), columnIndex, aValue);
    }


    /* *****************************************************************************************************************
     *                                    Tree Change methods
     *
     * Methods required for this model to listen for TreeModelListener events, or to manually notify the
     * model that a node has changed.  Note that the tree structure has *already* changed once these methods
     * are called, so a removed node will no longer be part of the actual tree even if it is currently being
     * displayed (so must be removed from display), or new nodes may have been added which are not currently displayed (so must be added).
     * This class can be subscribed to a DefaultTreeModel in order to respond to these tree events,
     * or you can call the change methods yourself if not using a tree model once the node structure has been changed.
     */

    /**
     * Refreshes the entire visible tree, rebuilding from the root upwards.
     * If you require a rebuild from scratch, then reset expanded nodes as well, as none of the existing expanded
     * nodes will be in the new tree. If you simply want to rebuild due to some node changes,
     * but would like to preserve expanded state of existing nodes if possible, then pass false to resetExpanded.
     *
     * @param resetExpanded Whether to reset expanded nodes - if true removes all node expansion (except root if not visible).
     */
    public void refreshTree(final boolean resetExpanded) {
        if (resetExpanded) {
            clearExpansions();   // wipe out any existing expansion, add the root as expanded if it's not visible.
        }
        buildVisibleNodes();   // rebuild all the visible nodes.
    }

    @Override
    public void treeNodesChanged(final TreeModelEvent e) {
        treeNodesChanged(getLastPathNode(e));
    }

    public void treeNodesChanged(final TreeNode nodeChanged) {
        final int visibleChildren = getVisibleChildCount(nodeChanged);
        if (visibleChildren > 0 && isVisible(nodeChanged)) {
            final int parentIndex = getModelIndex(nodeChanged); // has to do a linear scan of the visible nodes to locate the parent.
            fireTableRowsUpdated(parentIndex + 1, parentIndex + visibleChildren);
            //TODO: it may even be more efficient to just redraw the table than locate model indexes and notify of updates?  Profile.
            //fireTableDataChanged(); // forces table to redraw, structure hasn't changed.
        }
    }

    @Override
    public void treeNodesInserted(final TreeModelEvent e) {
        treeNodesInserted( getLastPathNode(e), e.getChildIndices());
    }

    /**
     * Informs the model that a new node was inserted into the parent node with the child index given.
     * @param parentNode The parent node that had a child inserted.
     * @param childIndex The index the child was inserted at.
     */
    public void treeNodeInserted(final TreeNode parentNode, final int childIndex) {
        if (parentNode == rootNode || (isExpanded(parentNode) && isVisible(parentNode))) {
            final int numInserted = insertChildNodeToModel(parentNode, childIndex);
            updateVisibleChildCounts(parentNode, numInserted);
        }
    }

    /**
     * Informs the model that new nodes were inserted into the parent node with the child indices given.
     *
     * @param parentNode The parent node that has new children.
     * @param childIndices The indices of the new children.
     */
    public void treeNodesInserted(final TreeNode parentNode, final int[] childIndices) {
        if (parentNode == rootNode || (isExpanded(parentNode) && isVisible(parentNode))) {
            final int numInserted = insertChildNodesToModel(parentNode, childIndices);
            updateVisibleChildCounts(parentNode, numInserted);
        }
    }

    @Override
    public void treeNodesRemoved(final TreeModelEvent e) {
        treeNodesRemoved(getLastPathNode(e), e.getChildIndices(), e.getChildren());
    }

    /**
     * Informs the model that child nodes have been removed from a parent node.
     *
     * @param previousParentNode The parent previously to the removed children.
     * @param childIndices The indices of the nodes removed.
     * @param removedChildren The child nodes which were removed.
     */
    public void treeNodesRemoved(final TreeNode previousParentNode, final int[] childIndices, final Object[] removedChildren) {
        if (previousParentNode == rootNode || (isVisible(previousParentNode) && isExpanded(previousParentNode))) {
            final int numRemoved = removeVisibleNodes(childIndices, removedChildren);
            updateVisibleChildCounts(previousParentNode, -numRemoved);
        }
    }

    /**
     * Informs the model that a child node was removed from a parent node.
     *
     * @param previousParentNode The parent previously to the removed child.
     * @param removedNode The removed child.
     */
    public void treeNodeRemoved(final TreeNode previousParentNode, final TreeNode removedNode) {
        if (previousParentNode == rootNode || (isVisible(previousParentNode) && isExpanded(previousParentNode))) {
            final int numRemoved = removeVisibleNode(removedNode);
            updateVisibleChildCounts(previousParentNode, -numRemoved);
        }
    }

    @Override
    public void treeStructureChanged(final TreeModelEvent e) {
        final TreeNode parentNode = getLastPathNode(e);
        if (e.getPath().length == 1 && parentNode != rootNode) { // only one object in path and not current root - set root.
            setRoot(parentNode);
        } else {
            treeStructureChanged(parentNode);
        }
    }

    /**
     * Informs the model that a node has changed its child structure a lot and so it should be rebuilt.
     *
     * @param changedNode The node whose child structure has changed.
     */
    public void treeStructureChanged(final TreeNode changedNode) {
        if (isVisible(changedNode)) { // is the node visible at all?
            if (isExpanded(changedNode)) { // are children of the node visible?
                rebuildVisibleChildren(changedNode);
            } else { // visible but no children visible - refresh the display of that node in the table.
                final int modelIndex = getModelIndex(changedNode);
                fireTableRowsUpdated(modelIndex, modelIndex);
            }
        }
    }

    //TODO: check that previous node expansions that may have incorrect child counts are corrected, given complete
    //      structure change.

    protected void rebuildVisibleChildren(final TreeNode changedNode) {
        int firstChildModelIndex = -1;

        // 1. remove any prior visible children:
        final int numOldChildren = getVisibleChildCount(changedNode);
        if (numOldChildren > 0) {
            firstChildModelIndex = getModelIndex(changedNode) + 1;
            removeDisplayedChildren(firstChildModelIndex, firstChildModelIndex + numOldChildren - 1);
        }

        // 2. insert any current children:
        int numNewChildren = 0;
        if (changedNode.getChildCount() > 0) {
            if (firstChildModelIndex < 0) { // don't recalculate model index if we already have it from removing.
                firstChildModelIndex = getModelIndex(changedNode) + 1;
            }
            numNewChildren = addChildrenToDisplay(firstChildModelIndex, changedNode);
        }

        // 3. update visible node counts.
        final int childRowsChanged = numNewChildren - numOldChildren;
        updateVisibleChildCounts(changedNode, childRowsChanged);
    }

    /**
     * Sets the root of the tree to a new root node.
     *
     * @param newRoot The new root node for the tree.
     * @throws IllegalArgumentException if newRoot is null.
     */
    public void setRoot(final TreeNode newRoot) {
        checkNull(newRoot, "newRoot");
        if (newRoot != rootNode) {
            rootNode = newRoot;
            clearExpansions();
            buildVisibleNodes();
        }
    }

    protected void checkNull(Object object, String fieldName) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null: " + fieldName);
        }
    }

    protected TreeNode getLastPathNode(TreeModelEvent e) {
        return (TreeNode) e.getPath()[e.getPath().length - 1];
    }

    protected int insertChildNodesToModel(final TreeNode parentNode, final int[] childIndices) {
        final int length = childIndices.length;
        int from = 0;
        int numInserted = 0;
        while (from < length) {
            final int to = findLastConsecutiveIndex(from, childIndices); // process all consecutive insertions together.
            if (from == to) { // just inserting a single node (that potentially also has children, so numinserted may be bigger than one).
                numInserted += insertChildNodeToModel(parentNode, childIndices[from]);
            } else {          // inserting a block of nodes.
                numInserted += insertChildNodesToModel(parentNode, childIndices, from, to);
            }
            from = to + 1;
        }
        return numInserted;
    }

    protected int insertChildNodeToModel(final TreeNode parentNode, final int childIndex) {
        final int insertPosition = getModelIndexAtInsertPosition(parentNode, childIndex, 1);
        if (insertPosition >= 0) { //TODO: why don't we throw an exception if the insertion indexes are out of bounds?
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

    protected int insertChildNodesToModel(final TreeNode parentNode, final int[] childIndices, final int from, final int to) {
        final int insertPosition = getModelIndexAtInsertPosition(parentNode, childIndices[from], to - from + 1);
        if (insertPosition > 0) { //TODO: why don't we throw an exception if the insertion indexes are out of bounds?
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

    protected int removeVisibleNodes(final int[] childIndices, final Object[] removedChildren) {
        final int length = childIndices.length;
        int from = 0;
        int numRemoved = 0;
        while (from < length) {
            final int to = findLastConsecutiveIndex(from, childIndices); // process all consecutive insertions together.
            if (from == to) { // just removing a single node (that potentially also has removedChildren, so numRemoved may be bigger than one).
                numRemoved += removeVisibleNode((TreeNode) removedChildren[from]);
            } else {          // removing a block of nodes.
                numRemoved += removeVisibleNodes(from, to, removedChildren);
            }
            from = to + 1;
        }
        return numRemoved;
    }

    protected int removeVisibleNode(final TreeNode removedNode) {
        final int modelIndex = getModelIndex(removedNode);
        if (modelIndex >= 0) {
            final int numChildren = getVisibleChildCount(removedNode);
            final int lastIndex = modelIndex + numChildren;
            displayedNodes.remove(modelIndex, lastIndex);
            fireTableRowsDeleted(modelIndex, lastIndex);
            return 1 + numChildren;
        }
        return 0; // wasn't visible after all - so nothing removed.
    }

    protected int removeVisibleNodes(final int from, final int to, final Object[] removedChildren) {
        final int modelIndex = getModelIndex((TreeNode) removedChildren[from]);
        if (modelIndex >= 0) {
            int numToRemove = 0;
            for (int index = from; index <= to; index++) {
                numToRemove += (getVisibleChildCount((TreeNode) removedChildren[index]) + 1);
            }
            final int lastIndex = modelIndex + numToRemove - 1;
            displayedNodes.remove(modelIndex, lastIndex);
            fireTableRowsDeleted(modelIndex, lastIndex);
            return numToRemove;
        }
        return 0; // wasn't visible after all - so nothing removed.
    }

    protected int getModelIndexAtInsertPosition(final TreeNode parentNode, final int firstInsertedIndex, final int numInsertions) {
        final int afterInsertionIndex = firstInsertedIndex + numInsertions;
        final int numChildren = parentNode.getChildCount();
        final int modelIndexToInsertAt;

        //TODO: error conditions check here?  Like if the number of children doesn't match the insertions being claimed...

        // No previous nodes (inserted into parent with no prior children):
        if (numInsertions == numChildren) {
            // Insert at the position just after the parent node.
            modelIndexToInsertAt = getModelIndex(parentNode) + 1;

        // If inserted at the end of existing children (after the last insertion is the size of the children)
        } else if (afterInsertionIndex == numChildren) {
            // previous last child is the one before the first inserted.
            final TreeNode previousChild = parentNode.getChildAt(firstInsertedIndex - 1);
            // insert one after last previous child and all its visible children. //TODO: null pointer exception.
            modelIndexToInsertAt = getModelIndex(previousChild) + getVisibleChildCount(previousChild) + 1;

        // Inserting before the end of existing children:
        } else {
            // insert at the index of the child the last inserted node displaced one on.
            modelIndexToInsertAt = getModelIndex(parentNode.getChildAt(afterInsertionIndex));
        }
        return modelIndexToInsertAt;
    }

    /**
     * Given an array of indices, and a position to start looking in them,
     * it returns the index of the last consecutive index, or the from index passed in if none exist.
     * For example, if we have [1, 2, 5, 6, 7, 8, 10], then calling this from 0, would return 1, as we have 1 and 2
     * consecutive, so the index of 2 is returned.  If we call it on 1, we get 1 returned, as the next index isn't 3.
     * If we call it on index 2, we get 5 returned, the index of 8 which is the last in a consecutive run of indexes
     * from 5.
     * <p>
     * This is used to optimise notifying the table when multiple child nodes are inserted or removed.  We can translate
     * the table notification messages for consecutive indices, as that maps to the table structure.  So we group
     * consecutive updates together.
     *
     * @param from The position in the indices to start looking in.
     * @param indices An array of index positions, some of which may be consecutive.
     * @return The index of the last consecutive index in the array, or from if there are no others.
     */
    protected int findLastConsecutiveIndex(final int from, final int[] indices) {
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


    /* *****************************************************************************************************************
     *                                          Node getters
     *
     * Methods to get nodes, converting between model and table indexes.
     */

    /**
     * @return the root node of the tree.
     */
    public TreeNode getRoot() {
        return rootNode;
    }

    /**
     * Gets the node at the model row.
     *
     * @param modelIndex The row in the display model to get the node for.
     * @return The node for the row in the (unsorted) model index.
     */
    public TreeNode getNodeAtRow(final int modelIndex) {
        return modelIndex >= 0 && modelIndex < displayedNodes.size() ? displayedNodes.get(modelIndex) : null;
    }

    /**
     * Gets the node in a bound JTable given a table row index.
     * The view row index can differ from the rows in this underlying model if the table is sorted or filtered.
     *
     * @param tableIndex The row in the table to get the node from.
     * @return The node at the tableRow position in the JTable.
     */
    public TreeNode getNodeAtTableRow(final int tableIndex) {
        return getNodeAtRow(getModelIndex(tableIndex));
    }

    /**
     * Gets the first node which is currently selected in the JTable, or null if no row is selected.
     *
     * @return the node which is currently selected in the JTable, or null if no row is selected.
     */
    public TreeNode getSelectedNode() {
        return table == null? null : getNodeAtTableRow(table.getSelectedRow());
    }

    /**
     * Gets a list of all the selected nodes in the JTable, or an empty list if no rows are selected.
     * @return a list of all the selected nodes in the JTable, or an empty list if no rows are selected.
     */
    public List<TreeNode> getSelectedNodes() {
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
    public int getModelIndex(final int tableIndex) {
        final RowSorter<? extends TableModel> rowSorter = table == null? null : table.getRowSorter();
        return rowSorter == null? tableIndex : rowSorter.convertRowIndexToModel(tableIndex);
    }

    /**
     * Gets the index in the model of a node, or -1 if it isn't visible or part of the current tree.
     *
     * @param node The node to get the model index for.
     * @return The index in the model of displayed nodes, or -1 if it isn't being displayed.
     */
    public int getModelIndex(final TreeNode node) {
        return displayedNodes.indexOf(node);
    }

    //TODO: does visibility checking in get Model Index cause problems?  Why have two methods?  Review getModelIndex.
    //      remember we took out a bunch of checking logic and just asked for whether it was in the displayed list.
    /**
     * Gets the index in the model of a node, or -1 if it isn't visible or part of the current tree.
     *
     * @param node the node to check
     * @return index of a node in the tree or -1 if not in the tree.
     */
    public int getModelIndexCheckVisible(final TreeNode node) {
        return isVisible(node) ? getModelIndex(node) : -1;
    }


    /* *****************************************************************************************************************
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
                displayedNodes.add(0, rootNode);
                fireTableRowsInserted(0, 0);
            } else {
                displayedNodes.remove(0);
                fireTableRowsDeleted(0, 0);
            }
        }
    }


    /* *****************************************************************************************************************
     *                                           Event listeners
     */

    /**
     * Adds a listener to TreeTableEvents, which inform the listener when a node is about to expand or collapse.
     *
     * @param listener The listener to be notified of tree expand or collapse events.
     */
    public void addExpandCollapseListener(final ExpandCollapseListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes a TreeTableEvent listener.
     *
     * @param listener The listener to remove.
     */
    public void removeExpandCollapseListener(final ExpandCollapseListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Adds a mouse listener for expand / collapse events to a JTable
     **/
    protected void addMouseListener() {
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
    protected void removeMouseListener() {
        if (table != null) {
            table.removeMouseListener(tableMouseListener);
        }
        tableMouseListener = null;
    }

    /**
     * Adds keyboard actions for + (expand), - (collapse) and space (toggle) selected node expansion.
     */
    protected void addKeyboardActions() {
        if (table != null) {
            final InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
            for (KeyStroke keyStroke : expandKeys) {
                inputMap.put(keyStroke, EXPAND_NODE_KEYSTROKE_KEY);
            }
            for (KeyStroke keyStroke : collapseKeys) {
                inputMap.put(keyStroke, COLLAPSE_NODE_KEYSTROKE_KEY);
            }
            for (KeyStroke keyStroke : toggleKeys) {
                inputMap.put(keyStroke, TOGGLE_EXPAND_NODE_KEYSTROKE_KEY);
            }
            final ActionMap actionMap = table.getActionMap();
            actionMap.put(EXPAND_NODE_KEYSTROKE_KEY, new TreeTableExpandAction());
            actionMap.put(COLLAPSE_NODE_KEYSTROKE_KEY, new TreeTableCollapseAction());
            actionMap.put(TOGGLE_EXPAND_NODE_KEYSTROKE_KEY, new TreeTableToggleExpandAction());
        }
    }

    /**
     * Removes keyboard actions for + (expand), - (collapse) and space (toggle) selected node expansion.
     */
    protected void removeKeyboardActions() {
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
            actionMap.remove(EXPAND_NODE_KEYSTROKE_KEY);
            actionMap.remove(COLLAPSE_NODE_KEYSTROKE_KEY);
            actionMap.remove(TOGGLE_EXPAND_NODE_KEYSTROKE_KEY);
        }
    }

    /**
     * Sets the click handler for the primary tree column.  If not set explicitly, it will either default
     * to the primary TableCellRenderer if it implements the TreeClickHandler interface, or a very rough default
     * handler will be used that estimates the likely node indent for a tree when processing clicks.
     *
     * @param clickHandler The TreeClickHandler to process expand/collapse events for the primary tree column.
     */
    public void setTreeClickHandler(final TreeClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }


    /* *****************************************************************************************************************
     *                                          Column methods
     */

    /**
     * Returns the table column model telling JTable what columns to display and their order.
     *
     * It calls createTableModel() if one has not yet been set, which is an abstract method which subclasses must implement.
     * It will also automatically set a TreeCellRenderer on the TableColumn with model index zero, if a renderer is not
     * already set on it.
     *
     * @return The TableColumnModel telling JTable what columns to display and their order.
     */
    public TableColumnModel getTableColumnModel() {
        if (columnModel == null) {
            columnModel = createTableColumnModel();
            setTreeCellRendererOnFirstColumnIfNotSet(columnModel);
        }
        return columnModel;
    }

    /**
     * Scans through the columns in the TableColumnModel looking for the column with a model index of 0
     * (TREE_COLUMN_INDEX).  If this column does not have a CellRenderer set on it, the default
     * TreeTableCellRenderer will be set.  The column with a model index of 0 is always the column
     * which renders the tree and responds to expand or collapse clicks.
     *
     * @param columnModel The TableColumnModel to scan.
     */
    protected void setTreeCellRendererOnFirstColumnIfNotSet(final TableColumnModel columnModel) {
        for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
            TableColumn column = columnModel.getColumn(columnIndex);
            if (column.getModelIndex() == TREE_COLUMN_INDEX && column.getCellRenderer() == null) {
                column.setCellRenderer(new TreeTableCellRenderer(this));
            }
        }
    }

    protected TableColumn createColumn(final int modelIndex, final Object headerValue) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, null, null);
    }

    protected TableColumn createColumn(final int modelIndex, final Object headerValue, final int width) {
        return createColumn(modelIndex, headerValue, width, null, null);
    }

    protected TableColumn createColumn(final int modelIndex, final Object headerValue,
                                       final TableCellRenderer cellRenderer) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, cellRenderer, null);
    }

    protected TableColumn createColumn(final int modelIndex, final Object headerValue, final int width,
                                       final TableCellRenderer cellRenderer) {
        return createColumn(modelIndex, headerValue, width, cellRenderer, null);
    }

    protected TableColumn createColumn(final int modelIndex, final Object headerValue,
                                       final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, cellRenderer, cellEditor);
    }

    protected TableColumn createColumn(final int modelIndex,  final Object headerValue, final int width,
                                       final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
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


    /* *****************************************************************************************************************
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
     * @param node The node to get a visible child count for.
     * @return The number of visible children under this node (including any other expanded nodes underneath).
     */
    public int getVisibleChildCount(final TreeNode node) {
        final Integer childCount = expandedNodeCounts.get(node);
        return childCount == null ? 0 : childCount;
    }

    /**
     * Sets a node to expand, if it isn't already expanded.
     * @param node The node to expand.
     */
    public void expandNode(final TreeNode node) {
        if (!isExpanded(node)) {
            toggleExpansion(node, getModelIndexCheckVisible(node));
        }
    }



    /**
     * Expands all children and subchildren of a parent node.
     *
     * @param parentNode The node to expand all children and subchildren.
     */
    public void expandAllChildren(final TreeNode parentNode) {
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            TreeNode child = parentNode.getChildAt(childIndex);
            expandNode(child);
            expandAllChildren(child);
        }
    }

    public void expandAllChildren(final TreeNode parentNode, final int depth) {
        if (depth > 0) {
            for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
                TreeNode child = parentNode.getChildAt(childIndex);
                expandNode(child);
                expandAllChildren(child, depth - 1);
            }
        }
    }

    /**
     * Collapses a node if it is already expanded.
     * @param node The node to collapse.
     */
    public void collapseNode(final TreeNode node) {
        if (isExpanded(node)) {
            toggleExpansion(node, getModelIndexCheckVisible(node));
        }
    }

    /**
     * Collapses expansion of all children (and sub children) of a parent node.
     *
     * @param parentNode The node to collapse all children.
     */
    public void collapseAllChildren(final TreeNode parentNode) {
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            TreeNode child = parentNode.getChildAt(childIndex);
            collapseNode(child);
            collapseAllChildren(child);
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
    protected void toggleExpansion(final TreeNode node, final int modelRow) {
        final boolean currentlyExpanded = isExpanded(node);
        if (node.getAllowsChildren()) {  //TODO check how get allows children affects tree building - can nodes have children but not allow them?
            if (listenersApprove(node, currentlyExpanded)) {
                if (modelRow >= 0 && modelRow < displayedNodes.size()) {
                    toggleVisibleExpansion(node, modelRow, currentlyExpanded); // deal with changes to visible nodes.
                } else {
                    toggleInvisibleExpansion(node, currentlyExpanded); // node not visible - just toggle it's expanded state.
                }
            }
        }
    }

    protected void toggleVisibleExpansion(final TreeNode node, final int modelRow, final boolean currentlyExpanded) {
        final int childrenChanged;
        if (currentlyExpanded) {
            childrenChanged = getVisibleChildCount(node);
            removeDisplayedChildren(modelRow, childrenChanged);
            expandedNodeCounts.remove(node);
            updateTreeChildCounts(node.getParent(), -childrenChanged);
        } else {
            childrenChanged = addChildrenToDisplay(modelRow, node);
            expandedNodeCounts.put(node, childrenChanged);
            updateTreeChildCounts(node.getParent(), childrenChanged);
        }

        // If we haven't removed or added any children, notify the table this one node may have changed.
        // This forces a visual refresh which may alter the rendering of the expand or collapse handles.
        if (childrenChanged == 0) {
            fireTableRowsUpdated(modelRow, modelRow);
        }
    }

    protected void toggleInvisibleExpansion(final TreeNode node, final boolean currentlyExpanded) {
        if (currentlyExpanded) {
            expandedNodeCounts.remove(node);
        } else {
            expandedNodeCounts.put(node, calculateVisibleChildNodes(node, true));
        }
    }

    /**
     * Updates the visible child count for a node with a number (negative or positive), and updates
     * all of its parents with the adjusted child count.
     *
     * @param node The node whose child count is changing by some negative or positive number.
     * @param delta The change in the child count for the node.
     */
    protected void updateVisibleChildCounts(final TreeNode node, final int delta) {
        if (delta != 0) {
            final Map<TreeNode, Integer> nodeCounts = expandedNodeCounts;
            final int newVisibleChildren = nodeCounts.get(node) + delta;
            nodeCounts.put(node, newVisibleChildren);
            updateTreeChildCounts(node.getParent(), newVisibleChildren);
        }
    }

    /**
     * Updates all child counds down the tree to root given a starting node and the change in child counts,
     * which can be negative or positive.
     *
     * @param startNode The node to start adjusting child counts for, down to the root node.
     * @param delta The amount to adjust the child counts, negative or positive.
     */
    protected void updateTreeChildCounts(final TreeNode startNode, final int delta) {
        if (delta != 0) {
            final Map<TreeNode, Integer> nodeCounts = expandedNodeCounts;
            TreeNode parentNode = startNode;
            while (parentNode != null) {
                final int parentChildCount = nodeCounts.get(parentNode);
                nodeCounts.put(parentNode, parentChildCount + delta);
                parentNode = parentNode.getParent();
            }
        }
    }

    /**
     * Clears the node expansion records for all nodes.
     * If the root node is not showing, then the root will be re-expanded (as otherwise nothing can be visible in the tree).
     */
    protected void clearExpansions() {
        expandedNodeCounts.clear();
        if (!showRoot) { // expand the root if it's not showing.
            expandNode(rootNode);
        }
    }

    /**
     * Returns true if this node is rooted in the model tree.
     *
     * @param node The node to check.
     * @return true if the node is rooted in the current tree.
     */
    protected boolean nodeInTree(final TreeNode node) {
        return getRoot(node) == rootNode;
    }

    /**
     * Gets the root node for a node (root node has a null parent).
     *
     * @param node The node to get the root for.
     * @return The root node for this node (can be the same node).
     */
    protected TreeNode getRoot(final TreeNode node) {
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
    protected void toggleExpansion(final MouseEvent evt) {
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

    /* *****************************************************************************************************************
     *                                         Visible node management.
     */

    /**
     * Returns true if the node is currently visible in the tree, or false otherwise.
     * Visibility doesn't mean the node is on-screen in the JTable, it means it is
     * currently displayable in the JTable and is tracked in this model as visible.
     *
     * @param node The node to check if visible.
     * @return true if the node is visible in the tree.
     */
    public boolean isVisible(final TreeNode node) {
        return node != null && (node == rootNode ? showRoot : parentsAreExpandedAndPartOfTree(node));
    }

    /**
     * Rebuilds the entire visible tree from the root.
     * Does not alter any prior node expansions, so they will remain expanded when the
     * tree is rebuilt.
     */
    protected void buildVisibleNodes() {
        displayedNodes.clear();
        if (showRoot) {
            displayedNodes.add(rootNode);
        }
        buildVisibleChildren(rootNode, displayedNodes);
    }

    protected void removeDisplayedChildren(final int modelRow, final int numberToRemove) {
        if (numberToRemove > 0) {
            final int firstChildRow = modelRow + 1;
            final int lastChildRow = firstChildRow + numberToRemove - 1;
            if (numberToRemove == 1) {
                displayedNodes.remove(firstChildRow);
            } else {
                displayedNodes.remove(firstChildRow, lastChildRow);
            }
            fireTableRowsDeleted(firstChildRow, lastChildRow);
        }
    }

    protected int addChildrenToDisplay(final int modelRow, final TreeNode node) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            final int firstChildRow = modelRow + 1;
            final List<TreeNode> newVisibleNodes = buildVisibleChildren(node);
            if (newVisibleNodes.size() == 1) {
                displayedNodes.add(firstChildRow, newVisibleNodes.get(0));
            } else {
                displayedNodes.addAll(firstChildRow, newVisibleNodes);
            }
            final int numNodes = newVisibleNodes.size();
            final int lastChildRow = firstChildRow + numNodes - 1;
            fireTableRowsInserted(firstChildRow, lastChildRow);
            return numNodes;
        }
        return 0;
    }

    /**
     * Builds a list of nodes which would be visible from the node given.
     * The node given does not yet have to be set as expanded in the model,
     * but any children must already be expanded if their children will be built.
     *
     * @param node The node to build its children and their visible children for.
     * @return A list of TreeNodes which are the visible children of that node and their children.
     */
    protected List<TreeNode> buildVisibleChildren(final TreeNode node) {
        if (node.getChildCount() > 0) {
            final List<TreeNode> children = new ArrayList<>();
            for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
                TreeNode child = node.getChildAt(childIndex);
                children.add(child);
                buildVisibleChildren(node.getChildAt(childIndex), children);
            }
            return children;
        }
        return Collections.emptyList();
    }

    /**
     * Builds a list of children which are visible from the node given.
     * The node given must already be expanded in the model for its visible children to be built.
     *
     * @param node The node to build visible children for.
     * @param visibleNodes A list of TreeNodes which are the visible children of that node and their children.
     */
    protected void buildVisibleChildren(final TreeNode node, final List<TreeNode> visibleNodes) { //TODO: should/could we update visible child counts as we build trees?
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
     * @param expandNode true if you want a count no matter whether it is currently expanded or not.
     * @return A count of all children which would be visible in this parent.
     */
    protected int calculateVisibleChildNodes(final TreeNode node, boolean expandNode) {
        int totalVisibleChildNodes = 0;
        if (expandNode || isExpanded(node)) {
            for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
                final TreeNode child = node.getChildAt(childIndex);
                totalVisibleChildNodes += (1 + calculateVisibleChildNodes(child, false));
            }
        }
        return totalVisibleChildNodes;
    }


    /**
     * @param node The node to check.
     * @return true if the node is in a visible chain of expanded parents and in the same tree as this model.
     */
    protected boolean parentsAreExpandedAndPartOfTree(final TreeNode node) {
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


    /* *****************************************************************************************************************
     *                                            Listener management
     */

    protected boolean listenersApprove(final TreeNode node, final boolean isCollapsing) {
        return isCollapsing ? listenersApprovedCollapseEvent(node) : listenersApprovedExpandEvent(node);
    }

    protected boolean listenersApprovedExpandEvent(final TreeNode node) {
        for (ExpandCollapseListener listener : eventListeners) {
            if (!listener.nodeExpanding(node)) {
                return false;
            }
        }
        return true;
    }

    protected boolean listenersApprovedCollapseEvent(final TreeNode node) {
        for (ExpandCollapseListener listener : eventListeners) {
            if (!listener.nodeCollapsing(node)) {
                return false;
            }
        }
        return true;
    }

    protected TreeClickHandler getClickHandler() {
        if (clickHandler == null) {
            clickHandler = getOrCreateTreeClickHandler();
        }
        return clickHandler;
    }

    protected TreeClickHandler getOrCreateTreeClickHandler() {
        final TableColumn column = getColumnWithModelIndex(TREE_COLUMN_INDEX);
        if (column != null) {
            TableCellRenderer renderer = column.getCellRenderer();
            if (renderer instanceof TreeClickHandler) {
                return (TreeClickHandler) renderer;
            }
        }
        return new TreeTableCellRenderer(this); // set a default click handler.
    }

    protected TableColumn getColumnWithModelIndex(final int modelIndex) {
        TableColumnModel model = getTableColumnModel();
        for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
            final TableColumn column = model.getColumn(columnIndex);
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
        return null;
    }

    /* *****************************************************************************************************************
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

    /* *****************************************************************************************************************
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
     * Used to build trees of MutableTreeNodes from a root object, and this method which provides the object children.
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
